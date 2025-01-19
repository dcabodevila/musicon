ALTER TABLE gestmusica.artista ADD tarifas_publicas bool DEFAULT true NOT NULL;

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='GESTION_AGRUPACION'));



INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='GESTION_AGRUPACION'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Administrador'), (select ID from gestmusica.permiso where codigo='GESTION_AGRUPACION'));


INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('GESTION_AGENCIAS', 'Acceso al panel de Agencias', 0);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='GESTION_AGENCIAS'));
INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Administrador'), (select ID from gestmusica.permiso where codigo='GESTION_AGENCIAS'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Administrador'), (select ID from gestmusica.permiso where codigo='USUARIOS'));


INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Administrador'), (select ID from gestmusica.permiso where codigo='ACCESO_PANEL_ADMIN'));

UPDATE gestmusica.permiso
SET codigo='ACCESO_PANEL_ADMIN', descripcion='Acceso al panel de administración', tipo_permiso=0
WHERE id=1;
UPDATE gestmusica.permiso
SET codigo='GESTION_AGRUPACION', descripcion='Acceso al panel de Artistas', tipo_permiso=0
WHERE id=2;
UPDATE gestmusica.permiso
SET codigo='GESTION_TARIFAS', descripcion='Gestionar tarifas de la agencia', tipo_permiso=1
WHERE id=3;
UPDATE gestmusica.permiso
SET codigo='OCUPACIONES', descripcion='Realizar ocupaciones en la agencia', tipo_permiso=1
WHERE id=4;
UPDATE gestmusica.permiso
SET codigo='USUARIOS', descripcion='Acceso al panel de gestión de usuarios', tipo_permiso=0
WHERE id=7;
UPDATE gestmusica.permiso
SET codigo='AGENCIA_CREAR', descripcion='Crear nuevas agencias', tipo_permiso=0
WHERE id=9;
UPDATE gestmusica.permiso
SET codigo='AGENCIA_EDITAR', descripcion='Editar características de la agencia', tipo_permiso=1
WHERE id=11;
UPDATE gestmusica.permiso
SET codigo='ARTISTAS_EDITAR', descripcion='Editar características de los artistas de la agencia', tipo_permiso=1
WHERE id=12;
UPDATE gestmusica.permiso
SET codigo='CONFIRMAR_OCUPACION', descripcion='Confirmar una ocupación pendiente', tipo_permiso=1
WHERE id=13;
UPDATE gestmusica.permiso
SET codigo='ANULAR_OCUPACION', descripcion='Anular una ocupación', tipo_permiso=1
WHERE id=14;
UPDATE gestmusica.permiso
SET codigo='TARIFA_ANUAL', descripcion='Sacar tarifa anual artistas', tipo_permiso=1
WHERE id=15;
UPDATE gestmusica.permiso
SET codigo='GESTION_ACCESOS', descripcion='Gestionar los accesos de la agencia', tipo_permiso=1
WHERE id=17;
UPDATE gestmusica.permiso
SET codigo='GESTION_AGENCIAS', descripcion='Acceso al panel de Agencias', tipo_permiso=0
WHERE id=18;


ALTER TABLE gestmusica.ocupacion ADD porcentaje_repre numeric(12, 2) DEFAULT 0 NOT NULL;
ALTER TABLE gestmusica.ocupacion ADD iva numeric(12, 2) DEFAULT 0 NOT NULL;
ALTER TABLE gestmusica.acceso ADD activo bool DEFAULT true NOT NULL;


