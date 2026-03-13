ALTER TABLE gestmusica.artista ADD is_sincronizar_odg boolean DEFAULT false NOT NULL;

CREATE TABLE gestmusica.odg_sincronizacion_tracking (
                                                        id BIGSERIAL PRIMARY KEY,
                                                        id_ejecucion VARCHAR(36) NOT NULL,
                                                        fecha_ejecucion TIMESTAMP NOT NULL,
                                                        ocupacion_id BIGINT NOT NULL,
                                                        artista_id BIGINT NOT NULL,
                                                        fecha_evento TIMESTAMP NOT NULL,
                                                        accion VARCHAR(20) NOT NULL,
                                                        resultado VARCHAR(20) NOT NULL,
                                                        message_type VARCHAR(20),
                                                        mensaje VARCHAR(1000)
);

CREATE INDEX idx_odg_sync_tracking_ejecucion ON gestmusica.odg_sincronizacion_tracking(id_ejecucion);
CREATE INDEX idx_odg_sync_tracking_ocupacion ON gestmusica.odg_sincronizacion_tracking(ocupacion_id);

ALTER TABLE gestmusica.artista
    ADD COLUMN solicitud_odg_pendiente boolean DEFAULT false NOT NULL;

ALTER TABLE gestmusica.artista
    ADD COLUMN solicitud_odg_aprobada boolean DEFAULT false NOT NULL;
