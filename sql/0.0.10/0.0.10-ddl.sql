alter table gestmusica.usuario add validado boolean NOT null default false;

CREATE TABLE gestmusica.listado (
    id bigint NOT NULL,
    solicitado_para character varying(255) NOT NULL,
	usuario_id bigint not null,
	municipio_id bigint not null,
	localidad character varying(255) NOT NULL,
	comentario character varying(255),
	tipo_ocupacion character varying(255) not null,
    fecha_desde DATE,
    fecha_hasta DATE,
    fecha1 DATE,
    fecha2 DATE,
    fecha3 DATE,
    fecha4 DATE,
    fecha5 DATE,
    fecha6 DATE,
    fecha7 DATE,
    ids_tipo_artista character varying(255),
    ids_comunidades character varying(255),
    fecha_creacion timestamp with time zone not null,
    CONSTRAINT fk_usuario_listado FOREIGN KEY (usuario_id) REFERENCES gestmusica.usuario (id),
    CONSTRAINT fk_municipio_listado FOREIGN KEY (municipio_id) REFERENCES gestmusica.municipio (id)
);

ALTER TABLE gestmusica.listado ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica."listado_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


ALTER TABLE gestmusica.listado
ADD CONSTRAINT pk_listado PRIMARY KEY (id);


CREATE TABLE gestmusica.listado_agencia (
 listado_id bigint not null,
 agencia_id bigint not null
);

-- Añadir la foreign key para listado_id
ALTER TABLE gestmusica.listado_agencia
ADD CONSTRAINT fk_listado_agencia_listado
FOREIGN KEY (listado_id) REFERENCES gestmusica.listado (id);

-- Añadir la foreign key para agencia_id
ALTER TABLE gestmusica.listado_agencia
ADD CONSTRAINT fk_listado_agencia_agencia
FOREIGN KEY (agencia_id) REFERENCES gestmusica.agencia (id);


CREATE TABLE gestmusica.listado_artista (
 listado_id bigint not null,
 artista_id bigint not null
);

-- Añadir la foreign key para listado_id
ALTER TABLE gestmusica.listado_artista
ADD CONSTRAINT fk_listado_artista_listado
FOREIGN KEY (listado_id) REFERENCES gestmusica.listado (id);

-- Añadir la foreign key para agencia_id
ALTER TABLE gestmusica.listado_artista
ADD CONSTRAINT fk_listado_artista_agencia
FOREIGN KEY (artista_id) REFERENCES gestmusica.artista (id);