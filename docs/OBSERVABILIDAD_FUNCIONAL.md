# Observabilidad funcional (eventos de negocio)

Para habilitar la emisión de eventos funcionales (OTel) configurá estas variables de entorno:

- `APP_OBSERVABILITY_FUNCTIONAL_ENABLED=true`  
  Activa la instrumentación de eventos funcionales.
- `APP_OBSERVABILITY_FUNCTIONAL_USER_KEY_SALT=<valor-secreto-estable>`  
  Sal usada para generar la clave pseudónima de usuario (`functional.user.key`).

Y, para exportar telemetría con OpenTelemetry Java Agent:

- `OTEL_SERVICE_NAME=gestmusica`
- `OTEL_EXPORTER_OTLP_ENDPOINT=<endpoint-otlp>`
- `OTEL_TRACES_EXPORTER=otlp`

Notas:
- No se emite PII en los eventos (no se envía email en claro).
- Si OpenTelemetry no está configurado o falla, el flujo de negocio sigue funcionando (fail-open).
