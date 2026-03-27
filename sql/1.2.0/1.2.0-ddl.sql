
-- Añadir columna excluir_sincronizacion_odg a la tabla ocupacion
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'gestmusica'
          AND table_name = 'ocupacion'
          AND column_name = 'excluir_sincronizacion_odg'
    ) THEN
        ALTER TABLE gestmusica.ocupacion ADD COLUMN excluir_sincronizacion_odg BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END $$;

-- Migration 1.3.0: Add TipoUsuario field to Usuario table
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'gestmusica'
          AND table_name = 'usuario'
          AND column_name = 'tipo_usuario'
    ) THEN
ALTER TABLE gestmusica.usuario
    ADD COLUMN tipo_usuario VARCHAR(20) NULL;
END IF;
END $$;


-- =============================================================================
-- v1.4.0 — Sistema de emails de reactivación para usuarios inactivos
-- =============================================================================

-- Campos de baja de email en tabla usuario
ALTER TABLE gestmusica.usuario ADD COLUMN IF NOT EXISTS email_baja BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE gestmusica.usuario ADD COLUMN IF NOT EXISTS email_baja_token VARCHAR(64);
ALTER TABLE gestmusica.usuario ADD COLUMN IF NOT EXISTS email_baja_fecha TIMESTAMPTZ;

-- Tabla de log de emails de reactivación
CREATE TABLE IF NOT EXISTS gestmusica.email_reactivacion_log (
                                                                 id          BIGSERIAL PRIMARY KEY,
                                                                 usuario_id  BIGINT NOT NULL REFERENCES gestmusica.usuario(id),
    segmento    VARCHAR(20) NOT NULL,
    fecha_envio TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado      VARCHAR(20) NOT NULL DEFAULT 'ENVIADO',
    template    VARCHAR(100)
    );


CREATE INDEX IF NOT EXISTS idx_reactivacion_usuario ON gestmusica.email_reactivacion_log(usuario_id);
CREATE INDEX IF NOT EXISTS idx_reactivacion_fecha   ON gestmusica.email_reactivacion_log(fecha_envio);
