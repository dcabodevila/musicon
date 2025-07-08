INSERT INTO gestmusica.permiso
(codigo, descripcion, tipo_permiso)
VALUES('DOCUMENTACION_DESCARGAR', 'Descargar documentación del artista', 2);

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ADMIN'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION_DESCARGAR'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='AGENCIA'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION_DESCARGAR'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='REPRE'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION_DESCARGAR'));

INSERT INTO gestmusica.rol_permisos
(rol_id, permiso_id)
VALUES((select id from gestmusica.rol where codigo='ARTISTA'), (select ID from gestmusica.permiso p where codigo='DOCUMENTACION_DESCARGAR'));


DELETE from gestmusica.rol_permisos rp where rp.permiso_id = (select id from gestmusica.permiso where codigo ='DOCUMENTACION') and rp.rol_id = (select id from gestmusica.rol r where r.codigo ='REPRE');