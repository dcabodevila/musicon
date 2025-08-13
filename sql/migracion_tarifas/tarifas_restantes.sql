WITH params AS (
  SELECT
    DATE '2025-01-01' AS d_start,
    DATE '2026-12-31' AS d_end,
    'usuario_sincronizacion'::text AS usr,
    0.00::numeric(38,2) AS imp_def
),
days AS (
  SELECT generate_series(
           (SELECT d_start FROM params),
           (SELECT d_end   FROM params),
           interval '1 day'
         )::date AS d
),
arts AS (
  SELECT id AS artista_id FROM gestmusica.artista
),
missing AS (
  SELECT ar.artista_id, d.d AS fecha_dia
  FROM arts ar
  CROSS JOIN days d
  WHERE NOT EXISTS (
    SELECT 1
    FROM gestmusica.tarifa t0
    WHERE t0.artista_id = ar.artista_id
      AND t0.fecha::date = d.d
  )
)
INSERT INTO gestmusica.tarifa (
  artista_id,
  importe,
  activo,
  fecha_modificacion,
  usuario_creacion,
  fecha,
  fecha_creacion,
  usuario_modificacion,
  matinal
)
SELECT
  artista_id,
  (SELECT imp_def FROM params) AS importe,
  TRUE AS activo,  -- siempre activo
  NULL::timestamptz AS fecha_modificacion,
  (SELECT usr FROM params) AS usuario_creacion,
  (fecha_dia)::timestamptz AS fecha,
  NOW() AS fecha_creacion,
  NULL::varchar(50) AS usuario_modificacion,
  FALSE AS matinal
FROM missing
ORDER BY artista_id, fecha_dia;