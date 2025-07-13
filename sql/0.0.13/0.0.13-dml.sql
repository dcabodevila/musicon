
INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('MENU_OCUPACIONES', 'Ver la entrada de menú Ocupaciones', 0);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='MENU_OCUPACIONES'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='MENU_OCUPACIONES'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='MENU_OCUPACIONES'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='MENU_OCUPACIONES'));

