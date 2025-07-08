
alter table gestmusica.ocupacion add usuario_conf_id bigint;

ALTER TABLE ONLY gestmusica.ocupacion
    ADD CONSTRAINT fk_ocupacion_usuario_conf_fk FOREIGN KEY (usuario_conf_id) REFERENCES gestmusica.usuario(id) NOT VALID;