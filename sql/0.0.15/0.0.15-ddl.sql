ALTER TABLE gestmusica.sincronizacion ADD procesado boolean default false;
ALTER TABLE gestmusica.sincronizacion ADD codigo_error varchar(1000) NULL;

ALTER TABLE gestmusica.artista ADD id_artista_gestmanager bigint NULL;


ALTER TABLE gestmusica.artista ADD condiciones_contratacion varchar(1000) NULL;
ALTER TABLE gestmusica.artista ADD biografia varchar(1000) NULL;


ALTER TABLE gestmusica.artista ADD permite_orquestas_de_galicia boolean default true;
