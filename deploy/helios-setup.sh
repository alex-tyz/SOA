#!/bin/sh
set -e

export _JAVA_OPTIONS="${_JAVA_OPTIONS:--XX:MaxHeapSize=768m -XX:MaxMetaspaceSize=256m}"

BASE="$HOME/soa-lab2"
PAYARA_VERSION="6.2025.11"
DRIVER_VERSION="42.7.4"
DOMAIN="spacemarine"
PORTBASE="${PORTBASE:-38696}"
DB_HOST="pg"
DB_NAME="studs"
DB_USER="$(id -un)"
DB_SCHEMA="soa_lab2"
APP="space-marine-service-1.0"

PAYARA_HOME="$BASE/payara6"
PASSWORD_FILE="$BASE/.admin-pw"
ADMIN_PORT=$((PORTBASE + 48))
HTTP_PORT=$((PORTBASE + 80))
HTTPS_PORT=$((PORTBASE + 81))
ASADMIN="$PAYARA_HOME/bin/asadmin --user admin --port $ADMIN_PORT --passwordfile $PASSWORD_FILE"

cd "$BASE"

if [ ! -d "$PAYARA_HOME" ]; then
    curl -fsSL -o payara.zip "https://repo1.maven.org/maven2/fish/payara/distributions/payara/$PAYARA_VERSION/payara-$PAYARA_VERSION.zip"
    tar -xf payara.zip
    rm payara.zip
fi

if [ ! -f "$PASSWORD_FILE" ]; then
    (umask 077; printf 'AS_ADMIN_PASSWORD=%s\nAS_ADMIN_MASTERPASSWORD=changeit\n' \
        "$(head -c 18 /dev/urandom | od -An -tx1 | tr -d ' \n')" > "$PASSWORD_FILE")
fi

if [ ! -d "$PAYARA_HOME/glassfish/domains/$DOMAIN" ]; then
    $ASADMIN create-domain --portbase "$PORTBASE" "$DOMAIN"
fi

if ! $ASADMIN list-domains | grep -q "$DOMAIN running"; then
    $ASADMIN start-domain "$DOMAIN"
fi

$ASADMIN set "configs.config.server-config.network-config.network-listeners.network-listener.admin-listener.address=127.0.0.1"

if ! $ASADMIN list-libraries | grep -q postgresql; then
    curl -fsSL -o postgresql.jar "https://repo1.maven.org/maven2/org/postgresql/postgresql/$DRIVER_VERSION/postgresql-$DRIVER_VERSION.jar"
    $ASADMIN add-library postgresql.jar
fi

export PAYARA_HOME DOMAIN ASADMIN
export CERT_CN="helios.cs.ifmo.ru"
export CERT_SAN="dns:localhost,dns:helios.cs.ifmo.ru"
sh "$BASE/payara-https.sh"

psql -h "$DB_HOST" -d "$DB_NAME" -v ON_ERROR_STOP=1 -c "CREATE SCHEMA IF NOT EXISTS $DB_SCHEMA"
if [ -z "$(psql -h "$DB_HOST" -d "$DB_NAME" -tA -c "SELECT to_regclass('$DB_SCHEMA.space_marine')")" ]; then
    PGOPTIONS="-c search_path=$DB_SCHEMA" psql -h "$DB_HOST" -d "$DB_NAME" -v ON_ERROR_STOP=1 -f "$BASE/space-marine.sql"
fi

if ! $ASADMIN list-password-aliases | grep -q dbpass; then
    DB_PASSWORD="$(awk -F: -v user="$DB_USER" '$4 == user {print $5; exit}' "$HOME/.pgpass")"
    if [ -z "$DB_PASSWORD" ]; then
        echo "no password for $DB_USER in ~/.pgpass" >&2
        exit 1
    fi
    (umask 077; { cat "$PASSWORD_FILE"; printf 'AS_ADMIN_ALIASPASSWORD=%s\n' "$DB_PASSWORD"; } > "$BASE/.alias-pw")
    "$PAYARA_HOME/bin/asadmin" --user admin --port "$ADMIN_PORT" --passwordfile "$BASE/.alias-pw" create-password-alias dbpass
    rm -f "$BASE/.alias-pw"
fi

if ! $ASADMIN list-jdbc-connection-pools | grep -q spacemarine-pool; then
    $ASADMIN create-jdbc-connection-pool \
        --datasourceclassname org.postgresql.ds.PGSimpleDataSource \
        --restype javax.sql.DataSource \
        --property "serverName=$DB_HOST:portNumber=5432:databaseName=$DB_NAME:user=$DB_USER:password=\${ALIAS=dbpass}:currentSchema=$DB_SCHEMA" \
        spacemarine-pool
    $ASADMIN create-jdbc-resource --connectionpoolid spacemarine-pool jdbc/spacemarine
fi
$ASADMIN ping-connection-pool spacemarine-pool

$ASADMIN deploy --force=true "$BASE/$APP.war"

echo "--- check"
curl -s --cacert "$BASE/spacemarine.crt" -o /dev/null -w "https %{http_code}\n" "https://localhost:$HTTPS_PORT/api/v1/space-marines" || true
curl -s -m 3 -o /dev/null -w "http %{http_code}\n" "http://localhost:$HTTP_PORT/api/v1/space-marines" || true
echo "HTTPS port: $HTTPS_PORT, certificate: $BASE/spacemarine.crt"
