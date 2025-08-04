alter table gestmusica.usuario add nombre_comercial varchar(150);
alter table gestmusica.usuario add telefono varchar(20);
alter table gestmusica.usuario add provincia_id bigint NOT NULL;

ALTER TABLE ONLY gestmusica.usuario
    ADD CONSTRAINT fk_usuario_provincia FOREIGN KEY (provincia_id) REFERENCES gestmusica.provincia(id) NOT VALID;

update gestmusica.usuario u set provincia_id =27 where provincia_id is null;
alter table gestmusica.usuario alter column provincia_id set not null;
    
    