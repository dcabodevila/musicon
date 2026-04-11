SELECT
    a.id,
    a.nombre AS artista,
    t.fecha AS fecha,
    COUNT(*) AS num_tarifas,
    STRING_AGG(t.id::text, ', ') AS ids
FROM gestmusica.tarifa t
         JOIN gestmusica.artista a ON t.artista_id = a.id
WHERE t.activo = true and t.fecha > current_timestamp
GROUP BY a.id, t.artista_id, a.nombre, t.fecha
HAVING COUNT(*) > 1
ORDER BY a.nombre, t.fecha;



select * from gestmusica.ocupacion o
where o.artista_id = 190
  and fecha = '2026-04-19 02:00:00.000 +0200'
;

select * from gestmusica.tarifa o
where o.artista_id = 190
  and fecha = '2026-08-22 02:00:00.000 +0200'
;