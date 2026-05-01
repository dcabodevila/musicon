-- =============================================================================
-- v1.4.1 — Agregar columna provincia_id a listado para persistir la provincia
-- =============================================================================

-- 1. Añadir columna provincia_id a la tabla listado
ALTER TABLE gestmusica.listado ADD COLUMN IF NOT EXISTS provincia_id BIGINT;

-- 2. Migrar datos existentes: copiar provincia desde el municipio asociado
UPDATE gestmusica.listado l
SET provincia_id = m.id_provincia
FROM gestmusica.municipio m
WHERE l.municipio_id = m.id
  AND l.provincia_id IS NULL;

-- 3. Agregar clave foránea a la tabla provincia
ALTER TABLE gestmusica.listado
    ADD CONSTRAINT fk_listado_provincia
    FOREIGN KEY (provincia_id) REFERENCES gestmusica.provincia(id);

-- Comentario para documentación
COMMENT ON COLUMN gestmusica.listado.provincia_id IS 'Provincia informada en el listado, persistida para independencia del municipio';
