INSERT INTO gestmusica.ccaa (id, nombre)
OVERRIDING SYSTEM VALUE
VALUES (22, 'Otras');

INSERT INTO gestmusica.provincia
(id, nombre, id_ccaa, abreviatura, nombre_orquestasdegalicia)
OVERRIDING SYSTEM VALUE
VALUES(55, 'Otras', 22, 'OTRO', 'Otras');

UPDATE gestmusica.provincia
SET nombre='Álava', id_ccaa=16, abreviatura='ALAV', nombre_orquestasdegalicia='Álava', id_provincia_legacy=1
WHERE id=1;
UPDATE gestmusica.provincia
SET nombre='Albacete', id_ccaa=8, abreviatura='ALBA', nombre_orquestasdegalicia='Albacete', id_provincia_legacy=2
WHERE id=2;
UPDATE gestmusica.provincia
SET nombre='Alicante', id_ccaa=10, abreviatura='ALIC', nombre_orquestasdegalicia='Alicante', id_provincia_legacy=3
WHERE id=3;
UPDATE gestmusica.provincia
SET nombre='Almería', id_ccaa=1, abreviatura='ALME', nombre_orquestasdegalicia='Almería', id_provincia_legacy=4
WHERE id=4;
UPDATE gestmusica.provincia
SET nombre='Ávila', id_ccaa=7, abreviatura='AVIL', nombre_orquestasdegalicia='Ávila', id_provincia_legacy=5
WHERE id=5;
UPDATE gestmusica.provincia
SET nombre='Badajoz', id_ccaa=11, abreviatura='BADJ', nombre_orquestasdegalicia='Badajoz', id_provincia_legacy=6
WHERE id=6;
UPDATE gestmusica.provincia
SET nombre='Baleares', id_ccaa=4, abreviatura='BALE', nombre_orquestasdegalicia='Islas Baleares', id_provincia_legacy=NULL
WHERE id=7;
UPDATE gestmusica.provincia
SET nombre='Barcelona', id_ccaa=9, abreviatura='BCN', nombre_orquestasdegalicia='Barcelona', id_provincia_legacy=8
WHERE id=8;
UPDATE gestmusica.provincia
SET nombre='Burgos', id_ccaa=7, abreviatura='BURG', nombre_orquestasdegalicia='Burgos', id_provincia_legacy=9
WHERE id=9;
UPDATE gestmusica.provincia
SET nombre='Cáceres', id_ccaa=11, abreviatura='CACE', nombre_orquestasdegalicia='Cáceres', id_provincia_legacy=10
WHERE id=10;
UPDATE gestmusica.provincia
SET nombre='Cádiz', id_ccaa=1, abreviatura='CADZ', nombre_orquestasdegalicia='Cádiz', id_provincia_legacy=11
WHERE id=11;
UPDATE gestmusica.provincia
SET nombre='Castellón', id_ccaa=10, abreviatura='CASTE', nombre_orquestasdegalicia='Castellón', id_provincia_legacy=12
WHERE id=12;
UPDATE gestmusica.provincia
SET nombre='Ciudad Real', id_ccaa=8, abreviatura='CDRL', nombre_orquestasdegalicia='Ciudad Real', id_provincia_legacy=13
WHERE id=13;
UPDATE gestmusica.provincia
SET nombre='Córdoba', id_ccaa=1, abreviatura='CORD', nombre_orquestasdegalicia='Córdoba', id_provincia_legacy=14
WHERE id=14;
UPDATE gestmusica.provincia
SET nombre='Coruña', id_ccaa=12, abreviatura='CORU', nombre_orquestasdegalicia='A Coruña', id_provincia_legacy=15
WHERE id=15;
UPDATE gestmusica.provincia
SET nombre='Cuenca', id_ccaa=8, abreviatura='CUEN', nombre_orquestasdegalicia='Cuenca', id_provincia_legacy=16
WHERE id=16;
UPDATE gestmusica.provincia
SET nombre='Girona', id_ccaa=9, abreviatura='GIRN', nombre_orquestasdegalicia='Gerona', id_provincia_legacy=17
WHERE id=17;
UPDATE gestmusica.provincia
SET nombre='Granada', id_ccaa=1, abreviatura='GRND', nombre_orquestasdegalicia='Granada', id_provincia_legacy=18
WHERE id=18;
UPDATE gestmusica.provincia
SET nombre='Guadalajara', id_ccaa=8, abreviatura='GUAD', nombre_orquestasdegalicia='Guadalajara', id_provincia_legacy=19
WHERE id=19;
UPDATE gestmusica.provincia
SET nombre='Gipuzkoa', id_ccaa=16, abreviatura='GIPZ', nombre_orquestasdegalicia='Guipúzcoa', id_provincia_legacy=20
WHERE id=20;
UPDATE gestmusica.provincia
SET nombre='Huelva', id_ccaa=1, abreviatura='HUEL', nombre_orquestasdegalicia='Huelva', id_provincia_legacy=21
WHERE id=21;
UPDATE gestmusica.provincia
SET nombre='Huesca', id_ccaa=2, abreviatura='HUES', nombre_orquestasdegalicia='Huesca', id_provincia_legacy=22
WHERE id=22;
UPDATE gestmusica.provincia
SET nombre='Jaén', id_ccaa=1, abreviatura='JAEN', nombre_orquestasdegalicia='Jaén', id_provincia_legacy=23
WHERE id=23;
UPDATE gestmusica.provincia
SET nombre='León', id_ccaa=7, abreviatura='LEON', nombre_orquestasdegalicia='León', id_provincia_legacy=24
WHERE id=24;
UPDATE gestmusica.provincia
SET nombre='Lleida', id_ccaa=9, abreviatura='LLEID', nombre_orquestasdegalicia='Lérida', id_provincia_legacy=25
WHERE id=25;
UPDATE gestmusica.provincia
SET nombre='La Rioja', id_ccaa=17, abreviatura='RIOJ', nombre_orquestasdegalicia='La Rioja', id_provincia_legacy=26
WHERE id=26;
UPDATE gestmusica.provincia
SET nombre='Lugo', id_ccaa=12, abreviatura='LUGO', nombre_orquestasdegalicia='Lugo', id_provincia_legacy=27
WHERE id=27;
UPDATE gestmusica.provincia
SET nombre='Madrid', id_ccaa=13, abreviatura='MADR', nombre_orquestasdegalicia='Madrid', id_provincia_legacy=28
WHERE id=28;
UPDATE gestmusica.provincia
SET nombre='Málaga', id_ccaa=1, abreviatura='MALG', nombre_orquestasdegalicia='Málaga', id_provincia_legacy=29
WHERE id=29;
UPDATE gestmusica.provincia
SET nombre='Murcia', id_ccaa=14, abreviatura='MURC', nombre_orquestasdegalicia='Murcia', id_provincia_legacy=30
WHERE id=30;
UPDATE gestmusica.provincia
SET nombre='Navarra', id_ccaa=15, abreviatura='NAVR', nombre_orquestasdegalicia='Navarra', id_provincia_legacy=31
WHERE id=31;
UPDATE gestmusica.provincia
SET nombre='Ourense', id_ccaa=12, abreviatura='OURE', nombre_orquestasdegalicia='Ourense', id_provincia_legacy=32
WHERE id=32;
UPDATE gestmusica.provincia
SET nombre='Asturias', id_ccaa=3, abreviatura='ASTU', nombre_orquestasdegalicia='Asturias', id_provincia_legacy=33
WHERE id=33;
UPDATE gestmusica.provincia
SET nombre='Palencia', id_ccaa=7, abreviatura='PALE', nombre_orquestasdegalicia='Palencia', id_provincia_legacy=34
WHERE id=34;
UPDATE gestmusica.provincia
SET nombre='Las Palmas', id_ccaa=5, abreviatura='PALM', nombre_orquestasdegalicia='Las Palmas', id_provincia_legacy=35
WHERE id=35;
UPDATE gestmusica.provincia
SET nombre='Pontevedra', id_ccaa=12, abreviatura='PONT', nombre_orquestasdegalicia='Pontevedra', id_provincia_legacy=36
WHERE id=36;
UPDATE gestmusica.provincia
SET nombre='Salamanca', id_ccaa=7, abreviatura='SALA', nombre_orquestasdegalicia='Salamanca', id_provincia_legacy=37
WHERE id=37;
UPDATE gestmusica.provincia
SET nombre='Tenerife', id_ccaa=5, abreviatura='TENE', nombre_orquestasdegalicia='Tenerife', id_provincia_legacy=101
WHERE id=38;
UPDATE gestmusica.provincia
SET nombre='Cantabria', id_ccaa=6, abreviatura='CANT', nombre_orquestasdegalicia='Cantabria', id_provincia_legacy=39
WHERE id=39;
UPDATE gestmusica.provincia
SET nombre='Segovia', id_ccaa=7, abreviatura='SEGO', nombre_orquestasdegalicia='Segovia', id_provincia_legacy=40
WHERE id=40;
UPDATE gestmusica.provincia
SET nombre='Sevilla', id_ccaa=1, abreviatura='SEVI', nombre_orquestasdegalicia='Sevilla', id_provincia_legacy=41
WHERE id=41;
UPDATE gestmusica.provincia
SET nombre='Soria', id_ccaa=7, abreviatura='SORI', nombre_orquestasdegalicia='Soria', id_provincia_legacy=42
WHERE id=42;
UPDATE gestmusica.provincia
SET nombre='Tarragona', id_ccaa=9, abreviatura='TARR', nombre_orquestasdegalicia='Tarragona', id_provincia_legacy=43
WHERE id=43;
UPDATE gestmusica.provincia
SET nombre='Teruel', id_ccaa=2, abreviatura='TERU', nombre_orquestasdegalicia='Teruel', id_provincia_legacy=44
WHERE id=44;
UPDATE gestmusica.provincia
SET nombre='Toledo', id_ccaa=8, abreviatura='TOLE', nombre_orquestasdegalicia='Toledo', id_provincia_legacy=45
WHERE id=45;
UPDATE gestmusica.provincia
SET nombre='Valencia', id_ccaa=10, abreviatura='VALE', nombre_orquestasdegalicia='Valencia', id_provincia_legacy=46
WHERE id=46;
UPDATE gestmusica.provincia
SET nombre='Valladolid', id_ccaa=7, abreviatura='VALL', nombre_orquestasdegalicia='Valladolid', id_provincia_legacy=47
WHERE id=47;
UPDATE gestmusica.provincia
SET nombre='Bizkaia', id_ccaa=16, abreviatura='VIZC', nombre_orquestasdegalicia='Vizcaya', id_provincia_legacy=48
WHERE id=48;
UPDATE gestmusica.provincia
SET nombre='Zamora', id_ccaa=7, abreviatura='ZAMO', nombre_orquestasdegalicia='Zamora', id_provincia_legacy=49
WHERE id=49;
UPDATE gestmusica.provincia
SET nombre='Zaragoza', id_ccaa=2, abreviatura='ZARA', nombre_orquestasdegalicia='Zaragoza', id_provincia_legacy=50
WHERE id=50;
UPDATE gestmusica.provincia
SET nombre='Ceuta', id_ccaa=18, abreviatura='CEUT', nombre_orquestasdegalicia='Ceuta', id_provincia_legacy=NULL
WHERE id=51;
UPDATE gestmusica.provincia
SET nombre='Melilla', id_ccaa=19, abreviatura='MELI', nombre_orquestasdegalicia='Melilla', id_provincia_legacy=NULL
WHERE id=52;
UPDATE gestmusica.provincia
SET nombre='Provisional', id_ccaa=20, abreviatura='OCUP', nombre_orquestasdegalicia='Provisional', id_provincia_legacy=113
WHERE id=53;
UPDATE gestmusica.provincia
SET nombre='Portugal', id_ccaa=21, abreviatura='PORT', nombre_orquestasdegalicia='Portugal', id_provincia_legacy=111
WHERE id=54;
UPDATE gestmusica.provincia
SET nombre='Otras', id_ccaa=22, abreviatura='OTRO', nombre_orquestasdegalicia='Otras', id_provincia_legacy=NULL
WHERE id=55;