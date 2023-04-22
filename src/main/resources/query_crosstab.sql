
SELECT *
FROM crosstab(
  'SELECT a.nombre, ag.nombre, t.fecha::date, to_char(t.importe, ''999999'') 
   FROM gestmusica.tarifa t inner join gestmusica.artista a on t.artista_id=a.id inner join gestmusica.agencia ag on a.agencia_id=ag.id
   WHERE  t.fecha BETWEEN ''2023-02-01'' AND ''2023-02-15'' AND t.activo
   AND t.fecha in (''2023-02-01'', ''2023-02-02'',''2023-02-03'')
   AND a.id in (6,7)	 
   AND a.activo
   ORDER BY 1, 2',
  'SELECT unnest(fecha_crosstab(''2023-02-01''::date, ''2023-02-15''::date))'
) AS ct ("Artista" text, "Agencia" text ,"dia1" text, "dia2" text, "dia3" text, "dia4" text, "dia5" text, "dia6" text
		, "dia7" text, "dia8" text, "dia9" text, "dia10" text, "dia11" text, "dia12" text, "dia13" text, "dia14" text, "dia15" text);
