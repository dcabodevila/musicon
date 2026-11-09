-- Passkeys (WebAuthn): acceso con huella, Face ID o Windows Hello.
-- Ejecutar ANTES de desplegar la version que activa las passkeys (Hibernate valida el esquema al arrancar).
-- Solo se guardan claves publicas; ningun dato biometrico sale del dispositivo del usuario.

CREATE TABLE IF NOT EXISTS gestmusica.webauthn_usuario (
    id              VARCHAR(255) PRIMARY KEY,
    usuario_id      BIGINT       NOT NULL UNIQUE REFERENCES gestmusica.usuario (id) ON DELETE CASCADE,
    nombre          VARCHAR(255) NOT NULL,
    nombre_visible  VARCHAR(255) NOT NULL
);

COMMENT ON TABLE gestmusica.webauthn_usuario IS 'Identificador WebAuthn (user handle) de cada usuario con passkeys';
COMMENT ON COLUMN gestmusica.webauthn_usuario.id IS 'User handle en base64url compartido con los autenticadores';

CREATE TABLE IF NOT EXISTS gestmusica.webauthn_credencial (
    credential_id                 VARCHAR(1024) PRIMARY KEY,
    webauthn_usuario_id           VARCHAR(255)  NOT NULL REFERENCES gestmusica.webauthn_usuario (id) ON DELETE CASCADE,
    public_key                    BYTEA         NOT NULL,
    signature_count               BIGINT        NOT NULL,
    uv_initialized                BOOLEAN       NOT NULL,
    backup_eligible               BOOLEAN       NOT NULL,
    backup_state                  BOOLEAN       NOT NULL,
    credential_type               VARCHAR(50),
    authenticator_transports      VARCHAR(255),
    attestation_object            BYTEA,
    attestation_client_data_json  BYTEA,
    etiqueta                      VARCHAR(255)  NOT NULL,
    fecha_creacion                TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_ultimo_uso              TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_webauthn_credencial_usuario
    ON gestmusica.webauthn_credencial (webauthn_usuario_id);

COMMENT ON TABLE gestmusica.webauthn_credencial IS 'Passkeys registradas (solo la clave publica de cada dispositivo)';
COMMENT ON COLUMN gestmusica.webauthn_credencial.etiqueta IS 'Nombre del dispositivo que elige el usuario (p. ej. iPhone de Juan)';
