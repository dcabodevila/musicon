--Ocupaciones activas con tarifas inactivas => No salen en listado
select * from gestmusica.ocupacion o inner join gestmusica.tarifa t on o.tarifa_id = t.id
where o.activo and o.estado_id = 1 and t.activo = false;

select * from gestmusica.ocupacion o
where o.artista_id = 8
and fecha = '2026-09-05 02:00:00.000 +0200'
;

select * from gestmusica.tarifa o
where o.artista_id = 8
and fecha = '2026-09-05 02:00:00.000 +0200'
;

select a.id, a.nombre, t.fecha, count(t.id) from gestmusica.tarifa t
inner join gestmusica.artista a on t.artista_id =a.id
where fecha > current_date
and a.activo and t.activo
group by a.id, a.nombre, t.fecha
having count(t.id) >1;

