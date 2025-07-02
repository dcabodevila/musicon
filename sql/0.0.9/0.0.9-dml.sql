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
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION'));