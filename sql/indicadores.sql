
-- USUARIOS QUE NO ACCEDEN EN MAS DE X DIAS

WITH last_login AS (
  SELECT usuario_id, MAX(fecha_login) AS last_ts
  FROM gestmusica.registro_login
  GROUP BY 1
)
SELECT
  u.id AS usuario_id,
  r.nombre ,
  u.nombre || ' ' || u.apellidos || ' - ' || u.nombre_comercial AS usuario_nombre,
  l.last_ts AS ultima_conexion
FROM last_login l
INNER JOIN gestmusica.usuario u ON u.id = l.usuario_id
inner join gestmusica.rol r on u.id_rol =r.id
WHERE l.last_ts < (now() - (:N || ' days')::interval)
and u.activo
ORDER BY l.last_ts;


--numero de logins
select r.nombre , u.nombre || ' ' || u.apellidos || ' - ' || u.nombre_comercial , count(rl.id) from gestmusica.registro_login rl inner join gestmusica.usuario u on rl.usuario_id = u.id
inner join gestmusica.rol r on u.id_rol =r.id
where u.activo
group by r.nombre, u.nombre || ' ' || u.apellidos || ' - ' || u.nombre_comercial ;