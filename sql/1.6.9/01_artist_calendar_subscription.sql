ALTER TABLE gestmusica.artista
    ADD COLUMN IF NOT EXISTS permitir_suscripcion_calendario BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS calendar_subscription_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS calendar_subscription_token_rotated_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_artista_calendar_subscription_token
    ON gestmusica.artista (calendar_subscription_token)
    WHERE calendar_subscription_token IS NOT NULL;

COMMENT ON COLUMN gestmusica.artista.permitir_suscripcion_calendario IS 'Activa la futura suscripción pública por artista mediante URL .ics revocable';
COMMENT ON COLUMN gestmusica.artista.calendar_subscription_token IS 'Token opaco y revocable para la URL pública de suscripción de calendario por artista';
COMMENT ON COLUMN gestmusica.artista.calendar_subscription_token_rotated_at IS 'Fecha de generación o rotación del token de suscripción de calendario';
