ALTER TABLE gestmusica.ocupacion ADD solo_matinal boolean DEFAULT false NULL;
ALTER TABLE gestmusica.provincia ADD abreviatura varchar(5) NULL;
UPDATE gestmusica.provincia
SET nombre='Albacete', id_ccaa=8, abreviatura='ALBA'
WHERE id=2;
UPDATE gestmusica.provincia
SET nombre='Almería', id_ccaa=1, abreviatura='ALME'
WHERE id=4;
UPDATE gestmusica.provincia
SET nombre='Asturias', id_ccaa=3, abreviatura='ASTU'
WHERE id=33;
UPDATE gestmusica.provincia
SET nombre='Ávila', id_ccaa=7, abreviatura='AVIL'
WHERE id=5;
UPDATE gestmusica.provincia
SET nombre='Badajoz', id_ccaa=11, abreviatura='BADJ'
WHERE id=6;
UPDATE gestmusica.provincia
SET nombre='Barcelona', id_ccaa=9, abreviatura='BCN'
WHERE id=8;
UPDATE gestmusica.provincia
SET nombre='Bizkaia', id_ccaa=16, abreviatura='VIZC'
WHERE id=48;
UPDATE gestmusica.provincia
SET nombre='Burgos', id_ccaa=7, abreviatura='BURG'
WHERE id=9;
UPDATE gestmusica.provincia
SET nombre='Cáceres', id_ccaa=11, abreviatura='CACE'
WHERE id=10;
UPDATE gestmusica.provincia
SET nombre='Cádiz', id_ccaa=1, abreviatura='CADZ'
WHERE id=11;
UPDATE gestmusica.provincia
SET nombre='Cantabria', id_ccaa=6, abreviatura='CANT'
WHERE id=39;
UPDATE gestmusica.provincia
SET nombre='Ceuta', id_ccaa=18, abreviatura='CEUT'
WHERE id=51;
UPDATE gestmusica.provincia
SET nombre='Ciudad Real', id_ccaa=8, abreviatura='CDRL'
WHERE id=13;
UPDATE gestmusica.provincia
SET nombre='Córdoba', id_ccaa=1, abreviatura='CORD'
WHERE id=14;
UPDATE gestmusica.provincia
SET nombre='Cuenca', id_ccaa=8, abreviatura='CUEN'
WHERE id=16;
UPDATE gestmusica.provincia
SET nombre='Gipuzkoa', id_ccaa=16, abreviatura='GIPZ'
WHERE id=20;
UPDATE gestmusica.provincia
SET nombre='Girona', id_ccaa=9, abreviatura='GIRN'
WHERE id=17;
UPDATE gestmusica.provincia
SET nombre='Granada', id_ccaa=1, abreviatura='GRND'
WHERE id=18;
UPDATE gestmusica.provincia
SET nombre='Guadalajara', id_ccaa=8, abreviatura='GUAD'
WHERE id=19;
UPDATE gestmusica.provincia
SET nombre='Huelva', id_ccaa=1, abreviatura='HUEL'
WHERE id=21;
UPDATE gestmusica.provincia
SET nombre='Huesca', id_ccaa=2, abreviatura='HUES'
WHERE id=22;
UPDATE gestmusica.provincia
SET nombre='Jaén', id_ccaa=1, abreviatura='JAEN'
WHERE id=23;
UPDATE gestmusica.provincia
SET nombre='León', id_ccaa=7, abreviatura='LEON'
WHERE id=24;
UPDATE gestmusica.provincia
SET nombre='Lugo', id_ccaa=12, abreviatura='LUGO'
WHERE id=27;
UPDATE gestmusica.provincia
SET nombre='Lleida', id_ccaa=9, abreviatura='LLEID'
WHERE id=25;
UPDATE gestmusica.provincia
SET nombre='Madrid', id_ccaa=13, abreviatura='MADR'
WHERE id=28;
UPDATE gestmusica.provincia
SET nombre='Málaga', id_ccaa=1, abreviatura='MALG'
WHERE id=29;
UPDATE gestmusica.provincia
SET nombre='Melilla', id_ccaa=19, abreviatura='MELI'
WHERE id=52;
UPDATE gestmusica.provincia
SET nombre='Murcia', id_ccaa=14, abreviatura='MURC'
WHERE id=30;
UPDATE gestmusica.provincia
SET nombre='Navarra', id_ccaa=15, abreviatura='NAVR'
WHERE id=31;
UPDATE gestmusica.provincia
SET nombre='Ourense', id_ccaa=12, abreviatura='OURE'
WHERE id=32;
UPDATE gestmusica.provincia
SET nombre='Palencia', id_ccaa=7, abreviatura='PALE'
WHERE id=34;
UPDATE gestmusica.provincia
SET nombre='Pontevedra', id_ccaa=12, abreviatura='PONT'
WHERE id=36;
UPDATE gestmusica.provincia
SET nombre='Salamanca', id_ccaa=7, abreviatura='SALA'
WHERE id=37;
UPDATE gestmusica.provincia
SET nombre='Segovia', id_ccaa=7, abreviatura='SEGO'
WHERE id=40;
UPDATE gestmusica.provincia
SET nombre='Sevilla', id_ccaa=1, abreviatura='SEVI'
WHERE id=41;
UPDATE gestmusica.provincia
SET nombre='Soria', id_ccaa=7, abreviatura='SORI'
WHERE id=42;
UPDATE gestmusica.provincia
SET nombre='Tarragona', id_ccaa=9, abreviatura='TARR'
WHERE id=43;
UPDATE gestmusica.provincia
SET nombre='Teruel', id_ccaa=2, abreviatura='TERU'
WHERE id=44;
UPDATE gestmusica.provincia
SET nombre='Toledo', id_ccaa=8, abreviatura='TOLE'
WHERE id=45;
UPDATE gestmusica.provincia
SET nombre='Valladolid', id_ccaa=7, abreviatura='VALL'
WHERE id=47;
UPDATE gestmusica.provincia
SET nombre='Zamora', id_ccaa=7, abreviatura='ZAMO'
WHERE id=49;
UPDATE gestmusica.provincia
SET nombre='Zaragoza', id_ccaa=2, abreviatura='ZARA'
WHERE id=50;
UPDATE gestmusica.provincia
SET nombre='Alicante', id_ccaa=10, abreviatura='ALIC'
WHERE id=3;
UPDATE gestmusica.provincia
SET nombre='Baleares', id_ccaa=4, abreviatura='BALE'
WHERE id=7;
UPDATE gestmusica.provincia
SET nombre='Castellón', id_ccaa=10, abreviatura='CASTE'
WHERE id=12;
UPDATE gestmusica.provincia
SET nombre='Coruña', id_ccaa=12, abreviatura='CORU'
WHERE id=15;
UPDATE gestmusica.provincia
SET nombre='Tenerife', id_ccaa=5, abreviatura='TENE'
WHERE id=38;
UPDATE gestmusica.provincia
SET nombre='Valencia', id_ccaa=10, abreviatura='VALE'
WHERE id=46;
UPDATE gestmusica.provincia
SET nombre='Álava', id_ccaa=16, abreviatura='ALAV'
WHERE id=1;
UPDATE gestmusica.provincia
SET nombre='Las Palmas', id_ccaa=5, abreviatura='PALM'
WHERE id=35;
UPDATE gestmusica.provincia
SET nombre='La Rioja', id_ccaa=17, abreviatura='RIOJ'
WHERE id=26;

ALTER TABLE gestmusica.ocupacion ADD CONSTRAINT ocupacion_tarifa_fk FOREIGN KEY (tarifa_id) REFERENCES gestmusica.tarifa(id);

ALTER TABLE gestmusica.artista ALTER COLUMN logo TYPE varchar(255) USING logo::varchar(255);

CREATE OR REPLACE FUNCTION gestmusica.fecha_crosstab_distintas(
    fecha1 date,
    fecha2 date,
    fecha3 date,
    fecha4 date,
    fecha5 date,
    fecha6 date,
    fecha7 date,
    fecha8 date,
    fecha9 date,
    fecha10 date,
    fecha11 date,
    fecha12 date,
    fecha13 date,
    fecha14 date,
    fecha15 date
)
RETURNS text[]
LANGUAGE plpgsql
AS $function$
DECLARE
    dates text[] := ARRAY[]::text[]; -- Inicializamos el arreglo vacío
BEGIN
    -- Agregar cada fecha al arreglo si no es NULL
    IF fecha1 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha1, 'YYYY-MM-DD'));
    END IF;
    IF fecha2 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha2, 'YYYY-MM-DD'));
    END IF;
    IF fecha3 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha3, 'YYYY-MM-DD'));
    END IF;
    IF fecha4 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha4, 'YYYY-MM-DD'));
    END IF;
    IF fecha5 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha5, 'YYYY-MM-DD'));
    END IF;
    IF fecha6 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha6, 'YYYY-MM-DD'));
    END IF;
    IF fecha7 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha7, 'YYYY-MM-DD'));
    END IF;
    IF fecha8 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha8, 'YYYY-MM-DD'));
    END IF;
    IF fecha9 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha9, 'YYYY-MM-DD'));
    END IF;
    IF fecha10 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha10, 'YYYY-MM-DD'));
    END IF;
    IF fecha11 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha11, 'YYYY-MM-DD'));
    END IF;
    IF fecha12 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha12, 'YYYY-MM-DD'));
    END IF;
    IF fecha13 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha13, 'YYYY-MM-DD'));
    END IF;
    IF fecha14 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha14, 'YYYY-MM-DD'));
    END IF;
    IF fecha15 IS NOT NULL THEN
        dates := array_append(dates, to_char(fecha15, 'YYYY-MM-DD'));
    END IF;

    -- Retornar el arreglo con las fechas
    RETURN dates;
END;
$function$
;

ALTER TABLE gestmusica.contacto ADD telefono2 varchar(15) NULL;
ALTER TABLE gestmusica.contacto ADD telefono3 varchar(15) NULL;
ALTER TABLE gestmusica.contacto ADD youtube varchar(200) NULL;

--Ejecutado