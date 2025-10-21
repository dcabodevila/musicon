#!/usr/bin/env bash
set -euo pipefail

JAVA_FLAGS=()

# Si hay host/puerto, activamos SOCKS5 para TODA la JVM (JDBC incluido)
if [[ -n "${SOCKS_HOST:-}" && -n "${SOCKS_PORT:-}" ]]; then
  JAVA_FLAGS+=("-DsocksProxyHost=${SOCKS_HOST}")
  JAVA_FLAGS+=("-DsocksProxyPort=${SOCKS_PORT}")
  # Autenticación opcional
  if [[ -n "${SOCKS_USER:-}" ]]; then
    JAVA_FLAGS+=("-DsocksProxyUser=${SOCKS_USER}")
    JAVA_FLAGS+=("-DsocksProxyPassword=${SOCKS_PASS:-}")
  fi
fi

# Sugerencia: permite inyectar flags extra por JAVA_OPTS (memoria, GC, TZ, etc.)
exec java ${JAVA_OPTS:-} "${JAVA_FLAGS[@]}" -jar /app/gestmusica.war
