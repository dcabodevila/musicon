INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('DOCUMENTACION', 'Subir y descargar documentación del artista', 2);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION'));

ALTER TABLE gestmusica.acceso
ADD COLUMN artista_id BIGINT NULL;

-- 2. Crear la foreign key constraint (no requerida, permite NULL)
ALTER TABLE gestmusica.acceso
ADD CONSTRAINT fk_acceso_artista
FOREIGN KEY (artista_id) REFERENCES gestmusica.artista(id);

-- 3. Crear índice para mejorar performance en consultas
CREATE INDEX idx_acceso_artista_id ON gestmusica.acceso(artista_id);
