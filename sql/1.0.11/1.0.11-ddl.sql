CREATE TABLE IF NOT EXISTS gestmusica.artista_ccaa (
    artista_id BIGINT NOT NULL,
    ccaa_id BIGINT NOT NULL,
    CONSTRAINT pk_artista_ccaa PRIMARY KEY (artista_id, ccaa_id),
    CONSTRAINT fk_artista_ccaa_artista FOREIGN KEY (artista_id) REFERENCES gestmusica.artista(id),
    CONSTRAINT fk_artista_ccaa_ccaa FOREIGN KEY (ccaa_id) REFERENCES gestmusica.ccaa(id)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE c.conname = 'fk_artista_ccaa_artista'
          AND n.nspname = 'gestmusica'
          AND t.relname = 'artista_ccaa'
    ) THEN
        ALTER TABLE gestmusica.artista_ccaa
            ADD CONSTRAINT fk_artista_ccaa_artista
            FOREIGN KEY (artista_id) REFERENCES gestmusica.artista(id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE c.conname = 'fk_artista_ccaa_ccaa'
          AND n.nspname = 'gestmusica'
          AND t.relname = 'artista_ccaa'
    ) THEN
        ALTER TABLE gestmusica.artista_ccaa
            ADD CONSTRAINT fk_artista_ccaa_ccaa
            FOREIGN KEY (ccaa_id) REFERENCES gestmusica.ccaa(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'gestmusica'
          AND table_name = 'artista_provincia'
    ) THEN
        INSERT INTO gestmusica.artista_ccaa (artista_id, ccaa_id)
        SELECT ap.artista_id, p.id_ccaa
        FROM gestmusica.artista_provincia ap
        JOIN gestmusica.provincia p ON p.id = ap.provincia_id
        ON CONFLICT DO NOTHING;
    END IF;
END $$;

INSERT INTO gestmusica.artista_ccaa (artista_id, ccaa_id)
SELECT a.id, c.id
FROM gestmusica.artista a
CROSS JOIN gestmusica.ccaa c
ON CONFLICT DO NOTHING;
