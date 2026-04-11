-- =============================================================================
-- ANÁLISIS DE ARTISTA ESPECÍFICO: Tarifas y Ocupaciones
-- 
-- Uso: psql -d gestmusica_db -v artista="'Arizona'" -f analisis_artista.sql
-- (usa ILIKE para búsqueda parcial, ej: 'Ari%')
--
-- EJECUTAR EN: psql -d gestmusica_db -v artista="'Arizona'" -f analisis_artista.sql
-- =============================================================================

\echo '========================================'
\echo 'ANÁLISIS DE ARTISTA'
\echo '========================================'

-- Datos del artista
\echo ''
\echo '[DATOS DEL ARTISTA]'
SELECT 
    a.id AS artista_id,
    a.nombre,
    ag.nombre AS agencia,
    a.activo
FROM gestmusica.artista a
JOIN gestmusica.agencia ag ON a.agencia_id = ag.id
WHERE a.nombre ILIKE :artista_nombre;

-- Todas las tarifas del artista
\echo ''
\echo '[TODAS LAS TARIFAS DEL ARTISTA]'
SELECT 
    t.id AS tarifa_id,
    t.fecha::date AS fecha,
    '$' || t.importe AS importe,
    CASE WHEN t.matinal THEN 'S' ELSE 'N' END AS matinal,
    t.activo,
    t.fecha_creacion,
    t.usuario_creacion
FROM gestmusica.tarifa t
WHERE t.artista_id = (SELECT id FROM gestmusica.artista WHERE nombre ILIKE :artista_nombre)
ORDER BY t.fecha DESC;

-- Todas las ocupaciones del artista
\echo ''
\echo '[TODAS LAS OCUPACIONES DEL ARTISTA]'
SELECT 
    o.id AS ocupacion_id,
    o.fecha::date AS fecha,
    o.tarifa_id,
    CASE o.estado_id 
        WHEN 1 THEN 'OCUPADO'
        WHEN 2 THEN 'RESERVADO'
        WHEN 3 THEN 'PENDIENTE'
        WHEN 4 THEN 'ANULADO'
        ELSE 'DESCONOCIDO'
    END AS estado,
    CASE WHEN o.matinal THEN 'S' ELSE 'N' END AS matinal,
    CASE WHEN o.solo_matinal THEN 'S' ELSE 'N' END AS solo_matinal,
    o.activo,
    o.fechacreacion
FROM gestmusica.ocupacion o
WHERE o.artista_id = (SELECT id FROM gestmusica.artista WHERE nombre ILIKE :artista_nombre)
ORDER BY o.fecha DESC;

-- Cruce completo: tarifa + ocupación
\echo ''
\echo '[CRUCE: TARIFA <-> OCUPACIÓN (cómo aparece en el crosstab)]'
SELECT 
    t.id AS tarifa_id,
    t.fecha::date AS fecha,
    '$' || t.importe AS tarifa_importe,
    CASE WHEN t.matinal THEN 'S' ELSE 'N' END AS tarifa_matinal,
    t.activo AS tarifa_activa,
    o.id AS ocupacion_id,
    o.estado_id,
    CASE o.estado_id 
        WHEN 1 THEN 'OCUPADO'
        WHEN 2 THEN 'RESERVADO'
        WHEN 3 THEN 'PENDIENTE'
        WHEN 4 THEN 'ANULADO'
        ELSE 'SIN OCUP'
    END AS ocupacion_estado,
    CASE WHEN o.matinal THEN 'S' ELSE 'N' END AS ocupacion_matinal,
    CASE 
        WHEN t.activo = true AND o.id IS NOT NULL AND o.estado_id IN (1,2,3) AND o.activo = true 
            THEN '✓ VISIBLE (tarifa + ocupación activa)'
        WHEN t.activo = true AND o.id IS NULL 
            THEN '⚠ TARIFA SIN OCUP (visible sin ocupación)'
        WHEN t.activo = true AND o.estado_id = 4 
            THEN '⚠ TARIFA + OCUP ANULADA'
        WHEN t.activo = false 
            THEN '✗ TARIFA INACTIVA'
        ELSE '?'
    END AS estado_en_listado
FROM gestmusica.tarifa t
LEFT JOIN gestmusica.ocupacion o ON o.tarifa_id = t.id AND o.activo = true
WHERE t.artista_id = (SELECT id FROM gestmusica.artista WHERE nombre ILIKE :artista_nombre)
ORDER BY t.fecha DESC;

-- Fechas con múltiples tarifas activas
\echo ''
\echo '[FECHAS CON MÚLTIPLES TARIFAS ACTIVAS]'
SELECT 
    t.fecha::date AS fecha,
    COUNT(*) AS num_tarifas,
    ARRAY_AGG(
        t.id || ' ($' || t.importe || ')' || CASE WHEN t.matinal THEN ' M' ELSE '' END
        ORDER BY t.id
    ) AS tarifas
FROM gestmusica.tarifa t
WHERE t.artista_id = (SELECT id FROM gestmusica.artista WHERE nombre ILIKE :artista_nombre)
    AND t.activo = true
GROUP BY t.fecha::date
HAVING COUNT(*) > 1
ORDER BY t.fecha::date;

-- Ocupaciones anuladas y sus tarifas
\echo ''
\echo '[OCUPACIONES ANULADAS Y SUS TARIFAS]'
SELECT 
    o.id AS ocupacion_id,
    o.fecha::date AS fecha,
    o.tarifa_id,
    '$' || t.importe AS tarifa_importe,
    CASE WHEN t.matinal THEN 'S' ELSE 'N' END AS tarifa_matinal,
    t.activo AS tarifa_activa,
    CASE 
        WHEN t.activo = true AND NOT EXISTS (
            SELECT 1 FROM gestmusica.ocupacion o2 
            WHERE o2.tarifa_id = t.id 
                AND o2.activo = true 
                AND o2.estado_id IN (1,2,3)
                AND o2.id != o.id
        ) THEN '⚠ HUÉRFANA (sin otra ocupación activa)'
        ELSE '✓ Compartida con otra ocupación'
    END AS analisis
FROM gestmusica.ocupacion o
JOIN gestmusica.tarifa t ON o.tarifa_id = t.id
WHERE o.artista_id = (SELECT id FROM gestmusica.artista WHERE nombre ILIKE :artista_nombre)
    AND o.estado_id = 4
    AND o.activo = true
ORDER BY o.fecha DESC;
