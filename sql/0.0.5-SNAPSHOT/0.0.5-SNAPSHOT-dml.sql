
INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('TARIFA_ANAL_CON_OCUPACION', 'Tarifa anual con ocupación de artistas', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='TARIFA_ANAL_CON_OCUPACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Agente ocupación'), (select ID from gestmusica.permiso where codigo='TARIFA_ANAL_CON_OCUPACION'));

UPDATE gestmusica.permiso
SET descripcion='Tarifa anual sin ocupación de artistas', tipo_permiso=1
WHERE  codigo='TARIFA_ANUAL';

INSERT INTO gestmusica.rol
( nombre, descripcion, tipo_rol)
VALUES( 'Tarifas anuales con ocupación', 'Rol con permiso para obtener tarifas anuales con y sin ocupación', 1);

INSERT INTO gestmusica.rol
( nombre, descripcion, tipo_rol)
VALUES( 'Tarifas anuales sin ocupación', 'Rol con permiso para obtener tarifas anuales sin ocupación', 1);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Tarifas anuales con ocupación'), (select ID from gestmusica.permiso where codigo='TARIFA_ANAL_CON_OCUPACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Tarifas anuales con ocupación'), (select ID from gestmusica.permiso where codigo='TARIFA_ANUAL'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select ID from gestmusica.rol where nombre='Tarifas anuales sin ocupación'), (select ID from gestmusica.permiso where codigo='TARIFA_ANUAL'));