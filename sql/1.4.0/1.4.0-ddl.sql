-- =============================================================================
-- v1.5.0 — Agregar campo evento_visible a ocupacion para permitir ocultar eventos
-- =============================================================================

-- Añadir columna evento_visible a la tabla ocupacion (default TRUE para mantener compatibilidad)
ALTER TABLE gestmusica.ocupacion ADD COLUMN IF NOT EXISTS evento_visible BOOLEAN NOT NULL DEFAULT TRUE;

-- Comentario para documentación
COMMENT ON COLUMN gestmusica.ocupacion.evento_visible IS 'Indica si el evento se muestra públicamente en /eventos. TRUE=visible, FALSE=oculto';
