-- =============================================================================
-- v1.4.2 — Agregar coordenadas geográficas a municipios para visualización en mapa
-- =============================================================================

-- 1. Añadir columnas de coordenadas a la tabla municipio
ALTER TABLE gestmusica.municipio
    ADD COLUMN IF NOT EXISTS latitud DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS longitud DECIMAL(11, 8);

-- 2. Índice para búsquedas rápidas de municipios con/sin coordenadas
CREATE INDEX IF NOT EXISTS idx_municipio_latitud ON gestmusica.municipio(latitud);
CREATE INDEX IF NOT EXISTS idx_municipio_longitud ON gestmusica.municipio(longitud);

-- 3. Comentarios para documentación
COMMENT ON COLUMN gestmusica.municipio.latitud IS 'Latitud del centroide del municipio (WGS84)';
COMMENT ON COLUMN gestmusica.municipio.longitud IS 'Longitud del centroide del municipio (WGS84)';


ALTER TABLE gestmusica.provincia
    ADD COLUMN IF NOT EXISTS latitud_capital DECIMAL(10, 8),
    ADD COLUMN IF NOT EXISTS longitud_capital DECIMAL(11, 8);