CREATE TABLE gestmusica.mensaje (
    id BIGINT not NULL,
    id_usuario_remite BIGINT NOT NULL REFERENCES gestmusica.usuario(id),
    id_usuario_receptor BIGINT NOT NULL REFERENCES gestmusica.usuario(id),
    asunto VARCHAR(255) NOT NULL,
    mensaje TEXT NOT NULL,
    url_enlace VARCHAR(500),
    leido BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    fecha_leido TIMESTAMP WITH TIME ZONE,
    activo BOOLEAN DEFAULT TRUE,
    destacado BOOLEAN DEFAULT FALSE,
    imagen VARCHAR(255)
);

ALTER TABLE gestmusica.mensaje ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica.mensaje_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

CREATE INDEX idx_mensaje_usuario_receptor ON gestmusica.mensaje(id_usuario_receptor);
CREATE INDEX idx_mensaje_leido ON gestmusica.mensaje(leido);