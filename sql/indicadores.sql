
-- USUARIOS QUE NO ACCEDEN EN MAS DE X DIAS

WITH last_login AS (
  SELECT usuario_id, MAX(fecha_login) AS last_ts
  FROM gestmusica.registro_login
  GROUP BY 1
)
SELECT
  u.id AS usuario_id,
  u.nombre || ' ' || u.apellidos || ' - ' || u.nombre_comercial AS usuario_nombre,
  l.last_ts AS ultima_conexion
FROM last_login l
INNER JOIN gestmusica.usuario u ON u.id = l.usuario_id
WHERE l.last_ts < (now() - (:N || ' days')::interval)
ORDER BY l.last_ts;