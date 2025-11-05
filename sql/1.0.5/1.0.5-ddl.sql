ALTER TABLE gestmusica.ajustes ADD nombre varchar(50) DEFAULT 'Predeterminado' NULL;
ALTER TABLE gestmusica.ajustes ADD predeterminado boolean DEFAULT false not NULL;
update gestmusica.ajustes set predeterminado=true;