-- ============================================
-- Crear tabla registro_login con FK a usuario
-- ============================================

CREATE TABLE IF NOT EXISTS gestmusica.registro_login (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    fecha_login TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    direccion_ip VARCHAR(45),
    agente_usuario VARCHAR(500),

    -- Foreign Key: usuario_id -> usuario.id
    CONSTRAINT fk_registro_login_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES gestmusica.usuario(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);

-- ============================================
-- Crear índices para optimización
-- ============================================

CREATE INDEX IF NOT EXISTS idx_registro_login_usuario_fecha
    ON gestmusica.registro_login(usuario_id, fecha_login DESC);

CREATE INDEX IF NOT EXISTS idx_registro_login_fecha
    ON gestmusica.registro_login(fecha_login DESC);

CREATE INDEX IF NOT EXISTS idx_registro_login_usuario
    ON gestmusica.registro_login(usuario_id);

CREATE INDEX IF NOT EXISTS idx_registro_login_ip
    ON gestmusica.registro_login(direccion_ip);

-- ============================================
-- Documentación
-- ============================================

COMMENT ON TABLE gestmusica.registro_login
    IS 'Registro de accesos/logins realizados por los usuarios del sistema';

COMMENT ON COLUMN gestmusica.registro_login.id
    IS 'Identificador único del registro';

COMMENT ON COLUMN gestmusica.registro_login.usuario_id
    IS 'Identificador del usuario que realizó el login (FK hacia usuario.id)';

COMMENT ON COLUMN gestmusica.registro_login.fecha_login
    IS 'Fecha y hora exacta del login';

COMMENT ON COLUMN gestmusica.registro_login.direccion_ip
    IS 'Dirección IP desde donde se realizó el login';

COMMENT ON COLUMN gestmusica.registro_login.agente_usuario
    IS 'User-Agent completo del navegador/cliente';

COMMENT ON CONSTRAINT fk_registro_login_usuario ON gestmusica.registro_login
    IS 'Foreign Key: usuario_id referencia a usuario(id) - ON DELETE CASCADE, ON UPDATE CASCADE';

    DROP INDEX gestmusica.idx_registro_login_ip;
    ALTER TABLE gestmusica.registro_login DROP COLUMN direccion_ip;
    ALTER TABLE gestmusica.registro_login DROP COLUMN agente_usuario;
--

ALTER TABLE gestmusica.listado ALTER COLUMN municipio_id DROP NOT NULL;
ALTER TABLE gestmusica.listado ALTER COLUMN localidad DROP NOT NULL;
