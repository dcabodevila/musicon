INSERT INTO gestmusica.ccaa (id, nombre) OVERRIDING SYSTEM VALUE VALUES (20, 'Provisional');

INSERT INTO gestmusica.provincia (id, nombre, id_ccaa, abreviatura) VALUES (53,'Provisional', 20, 'OCUP');

INSERT INTO gestmusica.municipio (id, id_provincia, cod_municipio, dc, nombre) OVERRIDING SYSTEM VALUE VALUES (8117, 53, 0, 0, 'Provisional');