


INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT "ID" from musicon.rol where "NOMBRE" = 'Administrador'),
		(SELECT "ID" from musicon.permiso where "CODIGO"='ACCESO_PANEL_ADMIN')
	);

INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT "ID" from musicon.rol where "NOMBRE" = 'Administrador'),
		(SELECT "ID" from musicon.permiso where "CODIGO"='GESTION_AGRUPACION')
	);

INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT "ID" from musicon.rol where "NOMBRE" = 'Administrador'),
		(SELECT "ID" from musicon.permiso where "CODIGO"='GESTION_TARIFAS')
	);

INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT "ID" from musicon.rol where "NOMBRE" = 'Administrador'),
		(SELECT "ID" from musicon.permiso where "CODIGO"='OCUPACIONES')
	);


INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT "ID" from musicon.rol where "NOMBRE" = 'Administrador'),
		(SELECT "ID" from musicon.permiso where "CODIGO"='RESERVAS')
	);

INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT "ID" from musicon.rol where "NOMBRE" = 'Administrador'),
		(SELECT "ID" from musicon.permiso where "CODIGO"='LISTADOS')
	);
INSERT INTO musicon.rol_permisos(
	rol_id, permiso_id)
	VALUES (
		(SELECT id from musicon.rol where nombre = 'Administrador'),
		(SELECT id from musicon.permiso where codigo='USUARIOS')
	);