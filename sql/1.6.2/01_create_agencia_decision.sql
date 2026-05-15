CREATE TABLE IF NOT EXISTS gestmusica.agencia_decision (
    id BIGSERIAL PRIMARY KEY,
    agencia_id BIGINT NOT NULL,
    codigo_decision VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_activacion TIMESTAMP NULL,
    fecha_rechazo TIMESTAMP NULL,
    CONSTRAINT ck_agencia_decision_codigo_decision CHECK (codigo_decision IN ('PUBLICACION_EVENTOS')),
    CONSTRAINT ck_agencia_decision_estado CHECK (estado IN ('ACTIVADO', 'RECHAZADO')),
    CONSTRAINT uq_agencia_decision_agencia_codigo UNIQUE (agencia_id, codigo_decision),
    CONSTRAINT fk_agencia_decision_agencia FOREIGN KEY (agencia_id)
        REFERENCES gestmusica.agencia (id)
);

CREATE INDEX IF NOT EXISTS idx_agencia_decision_agencia_id
    ON gestmusica.agencia_decision (agencia_id);

CREATE INDEX IF NOT EXISTS idx_agencia_decision_codigo
    ON gestmusica.agencia_decision (codigo_decision);
