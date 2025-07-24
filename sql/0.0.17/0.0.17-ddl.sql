CREATE TABLE gestmusica.localidad (
    id bigint NOT NULL,
    nombre character varying(50) NOT NULL,
    id_municipio bigint
);

ALTER TABLE ONLY gestmusica.localidad
    ADD CONSTRAINT localidad_pkey PRIMARY KEY (id);

ALTER TABLE ONLY gestmusica.localidad
    ADD CONSTRAINT fk_localidad_municipio FOREIGN KEY (id_municipio) REFERENCES gestmusica.municipio(id) NOT VALID;

