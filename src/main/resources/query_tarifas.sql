
SELECT a.nombre as "Nombre", ag.nombre as "Agencia",
           CASE extract(dow from d.enero)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END || EXTRACT(day FROM d.enero::date)  as "Enero",  to_char(t1.importe, '999999')  , 
       CASE extract(dow from d.febrero)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.febrero::date) as "Febrero", to_char(t2.importe, '999999') , 
       CASE extract(dow from d.marzo)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.marzo::date) as "Marzo", to_char(t3.importe, '999999') ,
       CASE extract(dow from d.abril)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.abril::date) as "Abril", to_char(t4.importe, '999999') ,
       CASE extract(dow from d.mayo)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.mayo::date) as "Mayo", to_char(t5.importe, '999999') ,
       CASE extract(dow from d.junio)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.junio::date) as "Junio", to_char(t6.importe, '999999') ,
       CASE extract(dow from d.julio)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.julio::date) as "Julio", to_char(t7.importe, '999999') ,
       CASE extract(dow from d.agosto)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.agosto::date) as "Agosto", to_char(t8.importe, '999999') ,
       CASE extract(dow from d.septiembre)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.septiembre::date) as "Septiembre", to_char(t9.importe, '999999') ,
       CASE extract(dow from d.octubre)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.octubre::date) as "Octubre", to_char(t10.importe, '999999') ,
       CASE extract(dow from d.noviembre)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.noviembre::date) as "Noviembre", to_char(t11.importe, '999999') ,
       CASE extract(dow from d.diciembre)
        WHEN 0 THEN 'D'
        WHEN 1 THEN 'L'
        WHEN 2 THEN 'M'
        WHEN 3 THEN 'X'
        WHEN 4 THEN 'J'
        WHEN 5 THEN 'V'
        WHEN 6 THEN 'S'
    END ||EXTRACT(day FROM d.diciembre::date) as "Diciembre", to_char(t12.importe, '999999') 
FROM (
	SELECT 
		generate_series('2023-01-01'::date, '2023-01-31'::date, '1 day') AS enero,
		generate_series('2023-02-01'::date, '2023-02-28'::date, '1 day') AS febrero,	
		generate_series('2023-03-01'::date, '2023-03-31'::date, '1 day') AS marzo,
		generate_series('2023-04-01'::date, '2023-04-30'::date, '1 day') AS abril,
		generate_series('2023-05-01'::date, '2023-05-31'::date, '1 day') AS mayo,
		generate_series('2023-06-01'::date, '2023-06-30'::date, '1 day') AS junio,
		generate_series('2023-07-01'::date, '2023-07-31'::date, '1 day') AS julio,
		generate_series('2023-08-01'::date, '2023-08-31'::date, '1 day') AS agosto,
		generate_series('2023-09-01'::date, '2023-09-30'::date, '1 day') AS septiembre,
		generate_series('2023-10-01'::date, '2023-10-31'::date, '1 day') AS octubre,
		generate_series('2023-11-01'::date, '2023-11-30'::date, '1 day') AS noviembre,
		generate_series('2023-12-01'::date, '2023-12-31'::date, '1 day') AS diciembre

) d
CROSS JOIN gestmusica.artista a inner join gestmusica.agencia ag on a.agencia_id=ag.id
LEFT JOIN gestmusica.tarifa t1 ON t1.artista_id = a.id AND t1.fecha::date = d.enero::date AND t1.activo
LEFT JOIN gestmusica.tarifa t2 ON t2.artista_id = a.id AND t2.fecha::date = d.febrero::date AND t2.activo
LEFT JOIN gestmusica.tarifa t3 ON t3.artista_id = a.id AND t3.fecha::date = d.marzo::date AND t3.activo
LEFT JOIN gestmusica.tarifa t4 ON t4.artista_id = a.id AND t4.fecha::date = d.abril::date AND t4.activo
LEFT JOIN gestmusica.tarifa t5 ON t5.artista_id = a.id AND t5.fecha::date = d.mayo::date AND t5.activo
LEFT JOIN gestmusica.tarifa t6 ON t6.artista_id = a.id AND t6.fecha::date = d.junio::date AND t6.activo
LEFT JOIN gestmusica.tarifa t7 ON t7.artista_id = a.id AND t7.fecha::date = d.julio::date AND t7.activo
LEFT JOIN gestmusica.tarifa t8 ON t8.artista_id = a.id AND t8.fecha::date = d.agosto::date AND t8.activo
LEFT JOIN gestmusica.tarifa t9 ON t9.artista_id = a.id AND t9.fecha::date = d.septiembre::date AND t9.activo
LEFT JOIN gestmusica.tarifa t10 ON t10.artista_id = a.id AND t10.fecha::date = d.octubre::date AND t10.activo
LEFT JOIN gestmusica.tarifa t11 ON t11.artista_id = a.id AND t11.fecha::date = d.noviembre::date AND t11.activo
LEFT JOIN gestmusica.tarifa t12 ON t12.artista_id = a.id AND t12.fecha::date = d.noviembre::date AND t12.activo
WHERE a.id=7
ORDER BY 1, 2
;


