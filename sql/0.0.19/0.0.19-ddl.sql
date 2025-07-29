ALTER TABLE gestmusica.provincia ADD nombre_orquestasdegalicia varchar(25) NULL;


UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia=nombre;

UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia='A Coruña' where nombre='Coruña';


INSERT INTO gestmusica.ccaa (id, nombre)
OVERRIDING SYSTEM VALUE
VALUES (21, 'Portugal');

INSERT INTO gestmusica.provincia
(id, nombre, id_ccaa, abreviatura, nombre_orquestasdegalicia)
OVERRIDING SYSTEM VALUE
VALUES(54, 'Portugal', 21, 'PORT', 'Portugal');

UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia='Gerona' where nombre='Girona';

UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia='Guipúzcoa' where nombre='Gipuzkoa';

UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia='Lérida' where nombre='Lleida';


UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia='Lérida' where nombre='Lleida';

UPDATE gestmusica.provincia
SET nombre_orquestasdegalicia='Vizcaya' where nombre='Bizkaia';
