ALTER TABLE gestmusica.ocupacion ADD ID_OCUPACION_LEGACY INTEGER NULL;


ALTER TABLE gestmusica.sincronizacion ADD solo_matinal boolean DEFAULT false;
ALTER TABLE gestmusica.sincronizacion ADD matinal boolean DEFAULT false;

ALTER TABLE gestmusica.provincia ADD ID_PROVINCIA_LEGACY INTEGER NULL;

ALTER TABLE gestmusica.sincronizacion ALTER COLUMN estado TYPE varchar(25) USING estado::varchar(25);

CREATE INDEX idx_ocupacion_id_legacy
    ON gestmusica.ocupacion (id_ocupacion_legacy);

CREATE INDEX idx_artista_id_artista_gestmanager
    ON gestmusica.artista (id_artista_gestmanager);