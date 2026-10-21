#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
if [[ -f "$repo_dir/deploy/env.local.sh" ]]; then
    set -a
    # shellcheck source=/dev/null
    source "$repo_dir/deploy/env.local.sh"
    set +a
fi

required() {
    local name="$1"
    if [[ -z "${!name:-}" ]]; then
        printf 'Missing environment variable: %s\n' "$name" >&2
        exit 1
    fi
}

cli() {
    "$WILDFLY_HOME/bin/jboss-cli.sh" --connect --controller="${WILDFLY_CONTROLLER:-localhost:9990}" "$@"
}

exists() {
    cli --command="$1:read-resource" 2>/dev/null | grep -q '"outcome" => "success"'
}

case "${1:-}" in
    start)
        required WILDFLY_HOME
        required STARSHIP_DB_URL
        required STARSHIP_DB_USER
        required SPACE_MARINE_BASE_URL
        if [[ -n "${STARSHIP_SERVER_BASE_DIR:-}" ]]; then
            export JAVA_OPTS="${JAVA_OPTS:-} -Djboss.server.base.dir=$STARSHIP_SERVER_BASE_DIR"
        fi
        exec "$WILDFLY_HOME/bin/standalone.sh" -b "${STARSHIP_BIND_ADDRESS:-0.0.0.0}"
        ;;
    configure-tls)
        required WILDFLY_HOME
        required STARSHIP_TLS_HOST
        required STARSHIP_TLS_PASSWORD
        if [[ "$STARSHIP_TLS_HOST" =~ ^[0-9]{1,3}(\.[0-9]{1,3}){3}$ ]]; then
            IFS=. read -r -a octets <<< "$STARSHIP_TLS_HOST"
            for octet in "${octets[@]}"; do
                if (( 10#$octet > 255 )); then
                    printf 'STARSHIP_TLS_HOST must be a valid IPv4 address\n' >&2
                    exit 1
                fi
            done
            certificate_san="ip:$STARSHIP_TLS_HOST"
        elif [[ "$STARSHIP_TLS_HOST" =~ ^[A-Za-z0-9.-]+$ ]]; then
            certificate_san="dns:$STARSHIP_TLS_HOST"
        else
            printf 'STARSHIP_TLS_HOST must be a DNS name or IPv4 address\n' >&2
            exit 1
        fi
        if [[ ! "$STARSHIP_TLS_PASSWORD" =~ ^[A-Za-z0-9._-]{6,}$ ]]; then
            printf 'STARSHIP_TLS_PASSWORD must contain at least 6 safe characters\n' >&2
            exit 1
        fi

        keystore="${STARSHIP_SERVER_BASE_DIR:-$WILDFLY_HOME/standalone}/configuration/starship.p12"
        if [[ ! -f "$keystore" ]]; then
            keytool -genkeypair -alias starship -keyalg RSA -keysize 3072 \
                -validity 365 -storetype PKCS12 -keystore "$keystore" \
                -storepass "$STARSHIP_TLS_PASSWORD" \
                -dname "CN=$STARSHIP_TLS_HOST" -ext "SAN=$certificate_san" \
                -noprompt
        fi

        if ! exists '/subsystem=elytron/key-store=starshipKS'; then
            cli --command="/subsystem=elytron/key-store=starshipKS:add(path=starship.p12,relative-to=jboss.server.config.dir,credential-reference={clear-text=\"$STARSHIP_TLS_PASSWORD\"},type=PKCS12)"
        fi
        if ! exists '/subsystem=elytron/key-manager=starshipKM'; then
            cli --command="/subsystem=elytron/key-manager=starshipKM:add(key-store=starshipKS,credential-reference={clear-text=\"$STARSHIP_TLS_PASSWORD\"})"
        fi
        if ! exists '/subsystem=elytron/server-ssl-context=starshipSSC'; then
            cli --command='/subsystem=elytron/server-ssl-context=starshipSSC:add(key-manager=starshipKM,protocols=["TLSv1.3","TLSv1.2"])'
        fi
        if exists '/subsystem=undertow/server=default-server/https-listener=https'; then
            cli --command='/subsystem=undertow/server=default-server/https-listener=https:remove()'
        fi
        cli --command='/subsystem=undertow/server=default-server/https-listener=https:add(socket-binding=https,ssl-context=starshipSSC,enable-http2=true)'
        if exists '/subsystem=undertow/server=default-server/http-listener=default'; then
            cli --command='/subsystem=undertow/server=default-server/http-listener=default:remove()'
        fi
        cli --command=':reload'
        ;;
    deploy)
        required WILDFLY_HOME
        bash "$repo_dir/gradlew" -p "$repo_dir" :starship-service:war
        cli --command="deploy $repo_dir/starship-service/build/libs/starship-service.war --force"
        ;;
    *)
        printf 'Usage: %s {start|configure-tls|deploy}\n' "$0" >&2
        exit 2
        ;;
esac
