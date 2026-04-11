-- =============================================================================
-- MONITOR: Tarifas y Ocupaciones en Estado Conflicto
-- 
-- Este script detecta problemas en la gestión de tarifas y ocupaciones que
-- pueden causar que los listados (crosstab) muestren datos incorrectos.
--
-- EJECUTAR EN: psql -d gestmusica_db -f monitoreo_tarifas_ocupaciones.sql
-- =============================================================================

-- =============================================================================
-- 1. TARIFAS DUPLICADAS POR ARTISTA/FECHA
--    Premisa violada: "No pueden quedar 2 tarifas activas en la misma fecha"
-- =============================================================================

\echo '============================================================'
\echo '1. TARIFAS DUPLICADAS POR ARTISTA/FECHA'
\echo '   (2+ tarifas activas para el mismo artista en la misma fecha)'
\echo '============================================================'

SELECT 
    t.artista_id,
    a.nombre AS artista,
    t.fecha::date AS fecha,
    COUNT(*) AS num_tarifas,
    ARRAY_AGG(t.id ORDER BY t.id) AS tarifa_ids,
    ARRAY_AGG('$' || t.importe ORDER BY t.id) AS importes,
    ARRAY_AGG(CASE WHEN t.matinal THEN 'S' ELSE 'N' END ORDER BY t.id) AS matinales
FROM gestmusica.tarifa t
JOIN gestmusica.artista a ON t.artista_id = a.id
WHERE t.activo = true
GROUP BY t.artista_id, a.nombre, t.fecha::date
HAVING COUNT(*) > 1
ORDER BY a.nombre, t.fecha::date;

-- =============================================================================
-- 2. TARIFAS ACTIVAS CON IMPORTE = 0
--    Estas tarifas muestran "0" en los listados en vez del precio real
-- =============================================================================

\echo ''
\echo '============================================================'
\echo '2. TARIFAS ACTIVAS CON IMPORTE = 0'
\echo '   (Muestran "0" en los listados en vez del precio real)'
\echo '============================================================'

SELECT 
    t.id AS tarifa_id,
    t.artista_id,
    a.nombre AS artista,
    t.fecha::date AS fecha,
    t.importe,
    CASE WHEN t.matinal THEN 'S' ELSE 'N' END AS matinal,
    t.activo,
    o.id AS ocupacion_id,
    o.estado_id,
    CASE o.estado_id 
        WHEN 1 THEN 'OCUPADO'
        WHEN 2 THEN 'RESERVADO'
        WHEN 3 THEN 'PENDIENTE'
        WHEN 4 THEN 'ANULADO'
        ELSE 'DESCONOCIDO'
    END AS estado_ocupacion
FROM gestmusica.tarifa t
JOIN gestmusica.artista a ON t.artista_id = a.id
LEFT JOIN gestmusica.ocupacion o ON o.tarifa_id = t.id AND o.activo = true AND o.estado_id IN (1,2,3)
WHERE t.activo = true 
    AND t.importe = 0
ORDER BY a.nombre, t.fecha::date;

-- =============================================================================
-- 3. TARIFAS ACTIVAS SIN OCUPACIÓN ASOCIADA (HUÉRFANAS)
--    Tarifas activas que no tienen ninguna ocupación activa usándolas
--    NOTA: Puede ser intencional si el artista tiene tarifa pero no ocupación
-- =============================================================================

\echo ''
\echo '============================================================'
\echo '3. TARIFAS ACTIVAS SIN OCUPACIÓN ACTIVA ASOCIADA'
\echo '   (Tarifas huérfanas - verificar si son intencionales)'
\echo '============================================================'

SELECT 
    t.id AS tarifa_id,
    t.artista_id,
    a.nombre AS artista,
    t.fecha::date AS fecha,
    '$' || t.importe AS importe,
    CASE WHEN t.matinal THEN 'S' ELSE 'N' END AS matinal,
    t.activo
FROM gestmusica.tarifa t
JOIN gestmusica.artista a ON t.artista_id = a.id
WHERE t.activo = true
    AND NOT EXISTS (
        SELECT 1 FROM gestmusica.ocupacion o 
        WHERE o.tarifa_id = t.id 
            AND o.activo = true 
            AND o.estado_id IN (1,2,3)
    )
ORDER BY a.nombre, t.fecha::date;

-- =============================================================================
-- 4. OCUPACIONES ACTIVAS CON TARIFA INACTIVA
--    Ocupaciones que apuntan a una tarifa que ya no está activa
--    ESTO ES UN ERROR - la ocupación debería usar una tarifa activa
-- =============================================================================

\echo ''
\echo '============================================================'
\echo '4. OCUPACIONES ACTIVAS CON TARIFA INACTIVA'
\echo '   (ERROR: la ocupación apunta a una tarifa inactiva)'
\echo '============================================================'

SELECT 
    o.id AS ocupacion_id,
    o.artista_id,
    a.nombre AS artista,
    o.fecha::date AS fecha,
    o.tarifa_id,
    CASE o.estado_id 
        WHEN 1 THEN 'OCUPADO'
        WHEN 2 THEN 'RESERVADO'
        WHEN 3 THEN 'PENDIENTE'
        WHEN 4 THEN 'ANULADO'
        ELSE 'DESCONOCIDO'
    END AS estado_ocupacion,
    t.activo AS tarifa_activa,
    '$' || t.importe AS tarifa_importe
FROM gestmusica.ocupacion o
JOIN gestmusica.artista a ON o.artista_id = a.id
JOIN gestmusica.tarifa t ON o.tarifa_id = t.id
WHERE o.activo = true 
    AND o.estado_id IN (1,2,3)
    AND t.activo = false
ORDER BY a.nombre, o.fecha::date;

-- =============================================================================
-- 5. TARIFAS ACTIVAS SOLO CON OCUPACIONES ANULADAS
--    La única ocupación de la tarifa está anulada
-- =============================================================================

\echo ''
\echo '============================================================'
\echo '5. TARIFAS ACTIVAS SOLO CON OCUPACIONES ANULADAS'
\echo '   (La única ocupación de la tarifa está anulada)'
\echo '============================================================'

SELECT 
    t.id AS tarifa_id,
    t.artista_id,
    a.nombre AS artista,
    t.fecha::date AS fecha,
    '$' || t.importe AS importe,
    CASE WHEN t.matinal THEN 'S' ELSE 'N' END AS matinal,
    t.activo,
    COUNT(o.id) AS total_ocupaciones,
    COUNT(CASE WHEN o.estado_id IN (1,2,3) AND o.activo = true THEN 1 END) AS ocupaciones_activas,
    COUNT(CASE WHEN o.estado_id = 4 AND o.activo = true THEN 1 END) AS ocupaciones_anuladas,
    ARRAY_AGG(o.id ORDER BY o.id) AS ocupacion_ids,
    ARRAY_AGG(
        CASE o.estado_id 
            WHEN 1 THEN 'OCUPADO'
            WHEN 2 THEN 'RESERVADO'
            WHEN 3 THEN 'PENDIENTE'
            WHEN 4 THEN 'ANULADO'
            ELSE 'DESCONOCIDO'
        END ORDER BY o.id
    ) AS estados
FROM gestmusica.tarifa t
JOIN gestmusica.artista a ON t.artista_id = a.id
LEFT JOIN gestmusica.ocupacion o ON o.tarifa_id = t.id
WHERE t.activo = true
GROUP BY t.id, t.artista_id, a.nombre, t.fecha::date, t.importe, t.matinal, t.activo
HAVING COUNT(CASE WHEN o.estado_id IN (1,2,3) AND o.activo = true THEN 1 END) = 0
    AND COUNT(CASE WHEN o.estado_id = 4 AND o.activo = true THEN 1 END) > 0
ORDER BY a.nombre, t.fecha::date;

-- =============================================================================
-- 6. RESUMEN EJECUTIVO
-- =============================================================================

\echo ''
\echo '============================================================'
\echo 'RESUMEN EJECUTIVO'
\echo '============================================================'

\echo ''
\echo 'Tarjetas duplicadas (mismo artista/fecha):'
SELECT COUNT(*) AS total FROM (
    SELECT t.artista_id, t.fecha::date
    FROM gestmusica.tarifa t
    WHERE t.activo = true
    GROUP BY t.artista_id, t.fecha::date
    HAVING COUNT(*) > 1
) dup;

\echo ''
\echo 'Tarjetas con importe = 0:'
SELECT COUNT(*) AS total FROM gestmusica.tarifa WHERE activo = true AND importe = 0;

\echo ''
\echo 'Tarjetas huérfanas (sin ocupación activa):'
SELECT COUNT(*) AS total FROM gestmusica.tarifa t
WHERE t.activo = true
    AND NOT EXISTS (
        SELECT 1 FROM gestmusica.ocupacion o 
        WHERE o.tarifa_id = t.id 
            AND o.activo = true 
            AND o.estado_id IN (1,2,3)
    );

\echo ''
\echo 'Ocupaciones con tarjeta inactiva:'
SELECT COUNT(*) AS total FROM gestmusica.ocupacion o
JOIN gestmusica.tarifa t ON o.tarifa_id = t.id
WHERE o.activo = true AND o.estado_id IN (1,2,3) AND t.activo = false;

\echo ''
\echo '============================================================'
\echo 'FIN DEL MONITOREO'
\echo '============================================================'
