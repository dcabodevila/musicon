INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('DETALLE_OCUPACIONES', 'Ver los detalles de las ocupaciones en el calendario', 2);
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Administrador'), (select ID from gestmusica.permiso where codigo='DETALLE_OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Artista'), (select ID from gestmusica.permiso where codigo='DETALLE_OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Agencia'), (select ID from gestmusica.permiso where codigo='DETALLE_OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Agente Pro'), (select ID from gestmusica.permiso where codigo='DETALLE_OCUPACIONES'));

SELECT
    aa.usuario_id,
    aa.artista_id,
    (SELECT id FROM gestmusica.permiso WHERE codigo = 'DETALLE_OCUPACIONES'),
    aa.activo
FROM
    gestmusica.acceso_artista aa

JOIN
    gestmusica.permiso p ON aa.permiso_id = p.id
WHERE
    p.codigo = 'OCUPACIONES';


delete from gestmusica.rol_permisos rp where rp.permiso_id = (select id from gestmusica.permiso p where p.codigo ='ANULAR_OCUPACION') and rp.rol_id = (select id from gestmusica.rol r where r.codigo = 'ARTISTA');
delete from gestmusica.rol_permisos rp where rp.permiso_id = (select id from gestmusica.permiso p where p.codigo ='VER_DATOS_ECONOMICOS') and rp.rol_id = (select id from gestmusica.rol r where r.codigo = 'ARTISTA');

INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Agente Pro'), (select ID from gestmusica.permiso where codigo='ANULAR_OCUPACION'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Agente Pro'), (select ID from gestmusica.permiso where codigo='OCUPACIONES'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Agente Pro'), (select ID from gestmusica.permiso where codigo='VER_DATOS_ECONOMICOS'));


