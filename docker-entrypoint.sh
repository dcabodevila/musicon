#!/usr/bin/env bash
set -euo pipefail

echo "=== Iniciando configuración de túnel SOCKS5 ==="

# Variables requeridas con valores por defecto
FIXIE_SOCKS_HOST="${FIXIE_SOCKS_HOST:-}"
DB_HOST="${DB_HOST:-gestmusica.com}"
DB_PORT="${DB_PORT:-3306}"
LOCAL_PORT="${LOCAL_PORT:-3307}"

# Verificar si necesitamos túnel
if [[ -z "$FIXIE_SOCKS_HOST" ]]; then
    echo "FIXIE_SOCKS_HOST no configurado - arrancando sin túnel SOCKS5"
    exec java -jar /app/gestmusica.war
fi

echo "Configurando túnel SOCKS5 hacia $DB_HOST:$DB_PORT"
echo "FIXIE_SOCKS_HOST: $FIXIE_SOCKS_HOST"

# Parsear FIXIE_SOCKS_HOST (formato: socks5://user:pass@host:port)
if [[ "$FIXIE_SOCKS_HOST" =~ ^socks5://([^:]+):([^@]+)@([^:]+):([0-9]+)$ ]]; then
    FIXIE_USER="${BASH_REMATCH[1]}"
    FIXIE_PASS="${BASH_REMATCH[2]}"
    FIXIE_HOST="${BASH_REMATCH[3]}"
    FIXIE_PORT="${BASH_REMATCH[4]}"
    
    echo "Usuario Fixie: $FIXIE_USER"
    echo "Host Fixie: $FIXIE_HOST:$FIXIE_PORT"
else
    echo "ERROR: FIXIE_SOCKS_HOST debe tener formato socks5://user:pass@host:port"
    echo "Recibido: $FIXIE_SOCKS_HOST"
    exit 1
fi

# Verificar conectividad a Fixie
echo "Verificando conectividad a $FIXIE_HOST:$FIXIE_PORT..."
if ! nc -z -w5 "$FIXIE_HOST" "$FIXIE_PORT"; then
    echo "ERROR: No se puede conectar a $FIXIE_HOST:$FIXIE_PORT"
    exit 1
fi
echo "✅ Conectividad a Fixie OK"

echo "Iniciando túnel: localhost:$LOCAL_PORT -> SOCKS5($FIXIE_HOST:$FIXIE_PORT) -> $DB_HOST:$DB_PORT"

# SINTAXIS CORREGIDA para socat SOCKS5
socat TCP-LISTEN:${LOCAL_PORT},fork,reuseaddr \
      SOCKS4A:${FIXIE_HOST}:${DB_HOST}:${DB_PORT},socksport=${FIXIE_PORT},socksuser=${FIXIE_USER} &

SOCAT_PID=$!
echo "Túnel iniciado con PID: $SOCAT_PID"

# Función para cleanup al terminar
cleanup() {
    echo "Terminando túnel SOCKS5..."
    kill $SOCAT_PID 2>/dev/null || true
    wait $SOCAT_PID 2>/dev/null || true
}
trap cleanup EXIT

# Esperar a que el túnel esté listo
echo "Esperando que el túnel esté disponible..."
for i in $(seq 1 30); do
    if nc -z 127.0.0.1 ${LOCAL_PORT}; then
        echo "✅ Túnel listo en puerto $LOCAL_PORT"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "ERROR: Túnel no disponible después de 30 segundos"
        exit 1
    fi
    echo "Esperando... ($i/30)"
    sleep 1
done

# Verificar que socat sigue corriendo
if ! kill -0 $SOCAT_PID 2>/dev/null; then
    echo "ERROR: El proceso socat murió"
    exit 1
fi

echo "=== Iniciando aplicación Java ==="
exec java -jar /app/gestmusica.war