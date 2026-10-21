#!/bin/sh
set -e

PAYARA_HOME="${PAYARA_HOME:-/c/payara6}"
DOMAIN="${DOMAIN:-domain1}"
CERT_ALIAS="${CERT_ALIAS:-spacemarine}"
CERT_CN="${CERT_CN:-localhost}"
CERT_SAN="${CERT_SAN:-dns:localhost,ip:127.0.0.1}"
STOREPASS="${STOREPASS:-changeit}"
HTTP_LISTENER="${HTTP_LISTENER:-http-listener-1}"
HTTPS_LISTENER="${HTTPS_LISTENER:-http-listener-2}"

if [ -z "$ASADMIN" ]; then
    if [ -f "$PAYARA_HOME/bin/asadmin.bat" ]; then
        ASADMIN="$PAYARA_HOME/bin/asadmin.bat"
    else
        ASADMIN="$PAYARA_HOME/bin/asadmin"
    fi
fi

KEYSTORE="$PAYARA_HOME/glassfish/domains/$DOMAIN/config/keystore.p12"
CERT_FILE="$(dirname "$0")/$CERT_ALIAS.crt"
TRUSTSTORE="$(dirname "$0")/truststore.p12"
NET="configs.config.server-config.network-config"

if keytool -list -alias "$CERT_ALIAS" -keystore "$KEYSTORE" -storetype PKCS12 -storepass "$STOREPASS" >/dev/null 2>&1; then
    echo "certificate '$CERT_ALIAS' already exists in keystore"
else
    keytool -genkeypair -alias "$CERT_ALIAS" -keyalg RSA -keysize 2048 -validity 365 \
        -dname "CN=$CERT_CN" -ext "SAN=$CERT_SAN" \
        -storetype PKCS12 -keystore "$KEYSTORE" -storepass "$STOREPASS" -keypass "$STOREPASS"
fi

keytool -exportcert -rfc -alias "$CERT_ALIAS" -keystore "$KEYSTORE" -storetype PKCS12 \
    -storepass "$STOREPASS" -file "$CERT_FILE"

$ASADMIN set "$NET.protocols.protocol.$HTTPS_LISTENER.ssl.cert-nickname=$CERT_ALIAS"
$ASADMIN set "$NET.network-listeners.network-listener.$HTTP_LISTENER.enabled=false"
$ASADMIN stop-domain "$DOMAIN"
$ASADMIN start-domain "$DOMAIN"

echo "https listener: $HTTPS_LISTENER, plain http listener $HTTP_LISTENER disabled"
echo "public certificate: $CERT_FILE"

rm -f "$TRUSTSTORE"
keytool -importcert -noprompt -alias "$CERT_ALIAS" -file "$CERT_FILE" \
    -storetype PKCS12 -keystore "$TRUSTSTORE" -storepass "$STOREPASS"
echo "truststore for clients: $TRUSTSTORE"
