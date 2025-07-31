select o.id_ocupacion_legacy as id_ocupacion_legacy,o.id, p.nombre as provincia, m.nombre as municipio, o.poblacion, o.lugar ,o.fecha, to2.nombre as tipo_ocupacion,  oe.nombre as estado from gestmusica.ocupacion o
inner join gestmusica.provincia p on o.provincia_id =p.id
inner join gestmusica.tipo_ocupacion to2 on o.tipo_ocupacion_id = to2.id
inner join gestmusica.ocupacion_estado oe on o.estado_id = oe.id
left join gestmusica.municipio m on o.municipio_id = m.id
where o.id_ocupacion_legacy is not null;