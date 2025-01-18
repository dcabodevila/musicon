ALTER TABLE gestmusica.rol ADD tipo_rol int DEFAULT 0 NOT NULL;
ALTER TABLE gestmusica.permiso ADD tipo_permiso int DEFAULT 0 NOT NULL;

CREATE TABLE gestmusica.usuario_agencia_rol (
	id bigint not null,
    usuario_id bigint NOT NULL,
    agencia_id bigint NOT null,
    rol_id bigint NOT null
);

ALTER TABLE gestmusica.usuario_agencia_rol ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica.usuario_agencia_rol_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE gestmusica.usuario_agencia_rol
    ADD CONSTRAINT fk_usuario_agencia_rol_usu FOREIGN KEY (usuario_id) REFERENCES gestmusica.usuario(id) NOT VALID;

ALTER TABLE gestmusica.usuario_agencia_rol
    ADD CONSTRAINT fk_usuario_agencia_rol_age FOREIGN KEY (agencia_id) REFERENCES gestmusica.agencia(id) NOT VALID;

ALTER TABLE gestmusica.usuario_agencia_rol
    ADD CONSTRAINT fk_usuario_agencia_rol_rol FOREIGN KEY (rol_id) REFERENCES gestmusica.rol(id) NOT VALID;


CREATE TABLE gestmusica.restricciones_usuario_artista_permiso (
	id bigint not null,
    usuario_id bigint NOT NULL,
    artista_id bigint NOT null,
    permiso_id bigint NOT null
);

ALTER TABLE gestmusica.restricciones_usuario_artista_permiso ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica.restricciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE gestmusica.restricciones_usuario_artista_permiso
    ADD CONSTRAINT fk_restricciones_usuario FOREIGN KEY (usuario_id) REFERENCES gestmusica.usuario(id) NOT VALID;

ALTER TABLE gestmusica.restricciones_usuario_artista_permiso
    ADD CONSTRAINT fk_restricciones_artista FOREIGN KEY (artista_id) REFERENCES gestmusica.artista(id) NOT VALID;

ALTER TABLE gestmusica.restricciones_usuario_artista_permiso
    ADD CONSTRAINT fk_restricciones_permiso FOREIGN KEY (permiso_id) REFERENCES gestmusica.permiso(id) NOT VALID;

ALTER TABLE gestmusica.acceso ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica.acceso_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('AGENCIA_EDITAR', 'Editar características de la agencia', 1);
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='AGENCIA_EDITAR'));

INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('ARTISTAS_EDITAR', 'Editar características de los artistas de la agencia', 1);
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='ARTISTAS_EDITAR'));

INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='GESTION_TARIFAS'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='OCUPACIONES'));


INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='GESTION_TARIFAS'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='OCUPACIONES'));
INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('CONFIRMAR_OCUPACION', 'Confirmar una ocupación pendiente', 1);
INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('ANULAR_OCUPACION', 'Anular una ocupación', 1);
INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('TARIFA_ANUAL', 'Sacar tarifa anual artistas', 1);
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='CONFIRMAR_OCUPACION'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='ANULAR_OCUPACION'));
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='TARIFA_ANUAL'));

INSERT INTO gestmusica.permiso (codigo, descripcion, tipo_permiso) VALUES ('GESTION_ACCESOS', 'Gestionar los accesos de la agencia', 1);
INSERT INTO gestmusica.rol_permisos (rol_id, permiso_id) VALUES((select ID from gestmusica.rol where nombre='Representante'), (select ID from gestmusica.permiso where codigo='GESTION_ACCESOS'));