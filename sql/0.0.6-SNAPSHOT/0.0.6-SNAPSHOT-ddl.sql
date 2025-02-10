CREATE TABLE gestmusica.acceso_artista (
	id bigint not null,
    usuario_id bigint NOT NULL,
    artista_id bigint NOT null,
    permiso_id bigint NOT null
);

ALTER TABLE gestmusica.acceso_artista ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica.restricciones_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE gestmusica.acceso_artista
    ADD CONSTRAINT fk_restricciones_usuario FOREIGN KEY (usuario_id) REFERENCES gestmusica.usuario(id) NOT VALID;

ALTER TABLE gestmusica.acceso_artista
    ADD CONSTRAINT fk_restricciones_artista FOREIGN KEY (artista_id) REFERENCES gestmusica.artista(id) NOT VALID;

ALTER TABLE gestmusica.acceso_artista
    ADD CONSTRAINT fk_restricciones_permiso FOREIGN KEY (permiso_id) REFERENCES gestmusica.permiso(id) NOT VALID;

ALTER TABLE gestmusica.acceso_artista ADD activo boolean DEFAULT true NOT NULL;