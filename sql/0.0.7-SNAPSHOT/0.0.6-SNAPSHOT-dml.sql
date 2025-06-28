



INSERT INTO gestmusica.rol
(nombre, descripcion, tipo_rol)
VALUES('Agencia', 'Responsable de la agencia', 1);

update gestmusica.acceso set rol_id = (select id from gestmusica.rol where codigo='AGENCIA') where rol_id = (select id from gestmusica.rol where codigo='REPRE');

UPDATE gestmusica.permiso
SET codigo='VER_TARIFAS',descripcion = 'Ver las tarifas del artista'
WHERE codigo='GESTION_TARIFAS';
--ADMIN


INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='ACCESO_PANEL_ADMIN'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='USUARIOS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='GESTION_AGENCIAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='GESTION_AGRUPACION'));

--AGENCIA

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='GESTION_AGENCIAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='GESTION_AGRUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='GESTION_AGENCIAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='VER_TARIFAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='AGENCIA_EDITAR'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='ARTISTAS_EDITAR'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='CONFIRMAR_OCUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='ANULAR_OCUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='TARIFA_ANUAL'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='TARIFA_ANUAL_CON_OCUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='LISTADOS_CON_OCUPACION_PERMITIR'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='GESTION_ACCESOS'));


--REPRE

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='GESTION_AGRUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='GESTION_AGENCIAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='TARIFA_ANUAL'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='TARIFA_ANUAL_CON_OCUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='LISTADOS_CON_OCUPACION_PERMITIR'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='LISTADOS_SIN_OCUPACION_PERMITIR'));

--ARTISTA

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='GESTION_AGRUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='ARTISTAS_EDITAR'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='VER_TARIFAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='ANULAR_OCUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='TARIFA_ANUAL'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='TARIFA_ANUAL_CON_OCUPACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='VER_TARIFAS'));

UPDATE gestmusica.permiso
SET descripcion='Gestionar tarifas del artista'
WHERE codigo='GESTION_TARIFAS';

INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('ARTISTA_CREAR', 'Crear artistas dentro de la agencia', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='ARTISTA_CREAR'));

INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('CREAR_TARIFAS', 'Crear y modificar tarifas del artista', 2);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='CREAR_TARIFAS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='CREAR_TARIFAS'));


INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('RESERVAR_OCUPACION', 'Crear y modificar reservas de la agencia', 2);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='RESERVAR_OCUPACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='RESERVAR_OCUPACION'));

INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('MODIFICAR_OCUPACION_OTROS', 'Modificar ocupaciones y reservas de otros usuarios', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='MODIFICAR_OCUPACION_OTROS'));

INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('VER_DATOS_ECONOMICOS', 'Ver datos económicos de las ocupaciones de la agencia', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='VER_DATOS_ECONOMICOS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='VER_DATOS_ECONOMICOS'));

INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('VER_DATOS_ACTUACION', 'Ver lugar, horarios y observaciones de ocupaciones de la agencia', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='VER_DATOS_ACTUACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='VER_DATOS_ACTUACION'));
