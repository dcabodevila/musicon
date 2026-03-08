-- Tabla para trackear qué usuarios han leído las release notes de cada versión
CREATE TABLE IF NOT EXISTS gestmusica.release_notes_read (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    version VARCHAR(20) NOT NULL,
    fecha_lectura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_release_notes_read_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES gestmusica.usuario(id)
        ON DELETE CASCADE,
    CONSTRAINT uq_usuario_version UNIQUE (usuario_id, version)
);

-- Crear índice para mejorar las consultas
CREATE INDEX idx_release_notes_read_usuario_id ON gestmusica.release_notes_read(usuario_id);
CREATE INDEX idx_release_notes_read_version ON gestmusica.release_notes_read(version);

COMMENT ON TABLE gestmusica.release_notes_read IS 'Tabla para trackear qué usuarios han leído las release notes de cada versión';
COMMENT ON COLUMN gestmusica.release_notes_read.usuario_id IS 'ID del usuario que leyó las release notes';
COMMENT ON COLUMN gestmusica.release_notes_read.version IS 'Versión de las release notes leídas (ej: 1.0.9)';
COMMENT ON COLUMN gestmusica.release_notes_read.fecha_lectura IS 'Fecha y hora en que el usuario leyó las release notes';
