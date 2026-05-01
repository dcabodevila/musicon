-- Migration: Create performance indexes for listado queries
-- Version: 2.0.1
-- Date: 2026-05-01
-- Note: Using CONCURRENTLY to avoid locking tables during creation

-- Index for listado_agencia.agencia_id to optimize agency filtering
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_listado_agencia_agencia_id 
    ON gestmusica.listado_agencia(agencia_id);

-- Index for listado.fecha_creacion to optimize date range queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_listado_fecha_creacion 
    ON gestmusica.listado(fecha_creacion DESC);

-- Composite index for listado(activo, fecha_creacion) to optimize filtered date queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_listado_activo_fecha_creacion 
    ON gestmusica.listado(activo, fecha_creacion DESC);

-- Index for artista.agencia_id to optimize artist lookups by agency
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_artista_agencia_id 
    ON gestmusica.artista(agencia_id);

-- Analyze tables after index creation
ANALYZE gestmusica.listado_agencia;
ANALYZE gestmusica.listado;
ANALYZE gestmusica.artista;
