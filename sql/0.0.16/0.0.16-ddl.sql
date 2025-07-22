CREATE TABLE gestmusica.noticia (
    id bigint NOT NULL,
    usuario_id bigint,
    titulo character varying(255) NOT NULL,
    contenido text,
    url character varying(255),
    imagen character varying(255),
    fecha date NOT NULL,
    activo boolean NOT NULL DEFAULT true,
    destacada boolean NOT NULL DEFAULT false,
    leida boolean NOT NULL DEFAULT false,
    CONSTRAINT pk_noticia PRIMARY KEY (id)
);

-- Crear la secuencia para el id autoincremental
ALTER TABLE gestmusica.noticia ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica."noticia_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

-- Añadir la foreign key para usuario_id
ALTER TABLE gestmusica.noticia
ADD CONSTRAINT fk_noticia_usuario
FOREIGN KEY (usuario_id) REFERENCES gestmusica.usuario (id);

-- Crear índices para mejorar el rendimiento
CREATE INDEX idx_noticia_fecha ON gestmusica.noticia(fecha);
CREATE INDEX idx_noticia_activo ON gestmusica.noticia(activo);
CREATE INDEX idx_noticia_destacada ON gestmusica.noticia(destacada);

ALTER TABLE gestmusica.ocupacion ADD provisional boolean default false;
