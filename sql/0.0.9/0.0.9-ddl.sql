CREATE TABLE gestmusica.documento (
    id bigint PRIMARY KEY,
    nombre VARCHAR(255) NOT NULL,
    url TEXT NOT NULL,
    activo BOOLEAN DEFAULT TRUE,
    usuario_creacion_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    usuario_modificacion_id BIGINT,
    fecha_modificacion TIMESTAMP,
    artista_id BIGINT NOT NULL,
    CONSTRAINT fk_usuario_creacion FOREIGN KEY (usuario_creacion_id) REFERENCES gestmusica.usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_usuario_modificacion FOREIGN KEY (usuario_modificacion_id) REFERENCES gestmusica.usuario (id) ON DELETE SET NULL,
    CONSTRAINT fk_artista FOREIGN KEY (artista_id) REFERENCES gestmusica.artista (id) ON DELETE CASCADE
);

ALTER TABLE gestmusica.documento ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica.documento_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE gestmusica.documento
ADD COLUMN resource_type VARCHAR(20);

-- Actualizar registros existentes con valor por defecto 'raw'
UPDATE gestmusica.documento
SET resource_type = 'raw'
WHERE resource_type IS NULL;

-- Crear índice para mejorar rendimiento en consultas
CREATE INDEX idx_documento_resource_type ON documento(resource_type);