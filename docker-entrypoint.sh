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
    exit 1
fi

# Verificar conectividad a Fixie
echo "Verificando conectividad a $FIXIE_HOST:$FIXIE_PORT..."
if ! nc -z -w5 "$FIXIE_HOST" "$FIXIE_PORT"; then
    echo "ERROR: No se puede conectar a $FIXIE_HOST:$FIXIE_PORT"
    exit 1
fi
echo "✅ Conectividad a Fixie OK"

# Función para crear túnel - MÚLTIPLES ESTRATEGIAS
create_tunnel() {
    echo "Intentando crear túnel SOCKS5..."
    
    # ESTRATEGIA 1: netcat con SOCKS5 nativo
    if command -v nc >/dev/null && nc -h 2>&1 | grep -q "\-X"; then
        echo "Probando netcat con SOCKS5 nativo..."
        cat > /tmp/socks_handler_nc.sh << EOF
#!/bin/bash
exec nc -X 5 -x $FIXIE_HOST:$FIXIE_PORT $DB_HOST $DB_PORT
EOF
        chmod +x /tmp/socks_handler_nc.sh
        
        socat TCP-LISTEN:${LOCAL_PORT},fork,reuseaddr \
              SYSTEM:"/tmp/socks_handler_nc.sh" &
        SOCAT_PID=$!
        
        # Probar si funciona
        sleep 2
        if kill -0 $SOCAT_PID 2>/dev/null && nc -z 127.0.0.1 $LOCAL_PORT; then
            echo "✅ Túnel creado con netcat SOCKS5"
            return 0
        else
            echo "❌ Netcat SOCKS5 falló"
            kill $SOCAT_PID 2>/dev/null || true
        fi
    fi
    
    # ESTRATEGIA 2: connect-proxy
    if command -v connect-proxy >/dev/null; then
        echo "Probando connect-proxy..."
        cat > /tmp/socks_handler_connect.sh << EOF
#!/bin/bash
exec connect-proxy -S $FIXIE_HOST:$FIXIE_PORT $DB_HOST $DB_PORT
EOF
        chmod +x /tmp/socks_handler_connect.sh
        
        socat TCP-LISTEN:${LOCAL_PORT},fork,reuseaddr \
              SYSTEM:"/tmp/socks_handler_connect.sh" &
        SOCAT_PID=$!
        
        # Probar si funciona
        sleep 2
        if kill -0 $SOCAT_PID 2>/dev/null && nc -z 127.0.0.1 $LOCAL_PORT; then
            echo "✅ Túnel creado con connect-proxy"
            return 0
        else
            echo "❌ connect-proxy falló"
            kill $SOCAT_PID 2>/dev/null || true
        fi
    fi
    
    # ESTRATEGIA 3: socat nativo (si soporta SOCKS)
    echo "Probando socat nativo..."
    socat TCP-LISTEN:${LOCAL_PORT},fork,reuseaddr \
          SOCKS4A:${FIXIE_HOST}:${DB_HOST}:${DB_PORT},socksport=${FIXIE_PORT} &
    SOCAT_PID=$!
    
    # Probar si funciona
    sleep 2
    if kill -0 $SOCAT_PID 2>/dev/null && nc -z 127.0.0.1 $LOCAL_PORT; then
        echo "✅ Túnel creado con socat nativo"
        return 0
    else
        echo "❌ socat nativo falló"
        kill $SOCAT_PID 2>/dev/null || true
    fi
    
    # Si llegamos aquí, todos fallaron
    echo "❌ ERROR: No se pudo crear túnel con ninguna estrategia"
    return 1
}

# Crear el túnel
if ! create_tunnel; then
    echo "ERROR: Fallo creando túnel SOCKS5"
    exit 1
fi

echo "Túnel iniciado con PID: $SOCAT_PID"

# Función para cleanup
cleanup() {
    echo "Terminando túnel SOCKS5..."
    kill $SOCAT_PID 2>/dev/null || true
    wait $SOCAT_PID 2>/dev/null || true
}
trap cleanup EXIT

# Verificación final del túnel
echo "Verificación final del túnel..."
for i in $(seq 1 10); do
    if nc -zv 127.0.0.1 ${LOCAL_PORT}; then
        echo "✅ Túnel verificado y funcionando"
        break
    fi
    if [ $i -eq 10 ]; then
        echo "ERROR: Túnel no responde después de verificación"
        exit 1
    fi
    sleep 1
done

echo "=== Iniciando aplicación Java ==="
exec java -jar /app/gestmusica.war