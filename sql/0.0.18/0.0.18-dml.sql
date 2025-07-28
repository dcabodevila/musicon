INSERT INTO gestmusica.rol
(codigo, nombre, descripcion, tipo_rol)
VALUES('RESTRINGIR','Presupuestos restringidos', 'Usuario con presupuestos restringidos para los artistas de la agencia', 1);


INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('PRESUPUESTOS_RESTRINGIDOS', 'Prohibir sacar presupuestos de los artistas de la agencia', 2);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='RESTRINGIR'), (select ID from gestmusica.permiso p where codigo='PRESUPUESTOS_RESTRINGIDOS'));