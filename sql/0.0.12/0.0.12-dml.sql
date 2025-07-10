update gestmusica.permiso set tipo_permiso = 2 where codigo ='VER_DATOS_ECONOMICOS';

INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('MENU_LISTADOS', 'Ver las opciones del menú de listados', 0);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='MENU_LISTADOS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='MENU_LISTADOS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='MENU_LISTADOS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='MENU_LISTADOS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENTE'), (select ID from gestmusica.permiso p where codigo='MENU_LISTADOS'));