-- Migration: Drop listado_artista table and constraints
-- Version: 2.0.1
-- Date: 2026-05-01

-- Drop constraints first (if any exist outside the table definition)
ALTER TABLE IF EXISTS gestmusica.listado_artista
    DROP CONSTRAINT IF EXISTS fk_listado_artista_listado;

ALTER TABLE IF EXISTS gestmusica.listado_artista
    DROP CONSTRAINT IF EXISTS fk_listado_artista_artista;

ALTER TABLE IF EXISTS gestmusica.listado_artista
    DROP CONSTRAINT IF EXISTS pk_listado_artista;

ALTER TABLE IF EXISTS gestmusica.listado_artista
    DROP CONSTRAINT IF EXISTS uq_listado_artista;

-- Drop the table
DROP TABLE IF EXISTS gestmusica.listado_artista CASCADE;

-- Verify table no longer exists
-- SELECT * FROM information_schema.tables WHERE table_schema = 'gestmusica' AND table_name = 'listado_artista';
