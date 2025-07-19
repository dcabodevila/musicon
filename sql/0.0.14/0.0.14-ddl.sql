
CREATE TABLE gestmusica.sincronizacion (
    id BIGINT not NULL,
    id_artista BIGINT,
    descripcion TEXT,
    fecha VARCHAR(20),
    poblacion VARCHAR(100),
    municipio VARCHAR(100),
    provincia VARCHAR(100),
    pais VARCHAR(100),
    nombre_local VARCHAR(150),
    accion VARCHAR(50),
    estado CHAR(1),
    indicadores CHAR(3),
    fecha_recepcion VARCHAR(30)
);

ALTER TABLE gestmusica.sincronizacion ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME gestmusica."sincronizacion_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

ALTER TABLE gestmusica.sincronizacion ALTER COLUMN fecha_recepcion TYPE timestamptz USING fecha_recepcion::timestamptz;
ALTER TABLE gestmusica.sincronizacion ALTER COLUMN indicadores TYPE varchar(5) USING indicadores::varchar(5);
ALTER TABLE gestmusica.sincronizacion ALTER COLUMN accion TYPE varchar(200) USING accion::varchar(200);
ALTER TABLE gestmusica.sincronizacion ALTER COLUMN estado TYPE varchar(5) USING estado::varchar(5);

ALTER TABLE gestmusica.sincronizacion ADD caddatos varchar(1000) NULL;


