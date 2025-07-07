INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('AUDIENCIA_LISTADOS', 'Ver datos de audiencia de listados generados', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='AUDIENCIA_LISTADOS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='AUDIENCIA_LISTADOS'));