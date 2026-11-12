-- Límite de intentos fallidos por código de verificación (registro y recuperación de contraseña).
-- Ejecutar ANTES de desplegar la versión que lo incluye (Hibernate valida el esquema al arrancar).

ALTER TABLE gestmusica.codigo_verificacion
    ADD COLUMN IF NOT EXISTS intentos_fallidos INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN gestmusica.codigo_verificacion.intentos_fallidos IS
    'Intentos fallidos contra este código; al llegar a 5 se desactiva';

-- Desde esta versión solo queda un código activo por email y tipo.
-- Desactiva los códigos activos repetidos y conserva el más reciente de cada email y tipo.
UPDATE gestmusica.codigo_verificacion c
SET activo = FALSE
WHERE c.activo = TRUE
  AND EXISTS (SELECT 1
              FROM gestmusica.codigo_verificacion n
              WHERE n.email = c.email
                AND n.tipo = c.tipo
                AND n.activo = TRUE
                AND (n.fecha_creacion > c.fecha_creacion
                     OR (n.fecha_creacion = c.fecha_creacion AND n.id > c.id)));
