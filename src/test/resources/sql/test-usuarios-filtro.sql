-- Test data for UsuarioRepositoryFiltroTest
-- CCAA: 1=Madrid, 2=Cataluña
-- Provincia: 1=Madrid (CCAA 1), 2=Barcelona (CCAA 2)
-- Rol: 1=ADMIN, 2=AGENCIA, 3=ARTISTA

-- Clean up first (in reverse order of dependencies)
DELETE FROM gestmusica.usuario WHERE email LIKE '%@test.com';
DELETE FROM gestmusica.provincia WHERE id IN (1, 2);
DELETE FROM gestmusica.ccaa WHERE id IN (1, 2);
DELETE FROM gestmusica.rol WHERE id IN (1, 2, 3);

-- Insert CCAA
INSERT INTO gestmusica.ccaa (id, nombre) VALUES (1, 'Madrid') ON CONFLICT DO NOTHING;
INSERT INTO gestmusica.ccaa (id, nombre) VALUES (2, 'Cataluña') ON CONFLICT DO NOTHING;

-- Insert Provincias
INSERT INTO gestmusica.provincia (id, nombre, id_ccaa, abreviatura) VALUES (1, 'Madrid', 1, 'MAD') ON CONFLICT DO NOTHING;
INSERT INTO gestmusica.provincia (id, nombre, id_ccaa, abreviatura) VALUES (2, 'Barcelona', 2, 'BCN') ON CONFLICT DO NOTHING;

-- Insert Roles
INSERT INTO gestmusica.rol (id, nombre, descripcion, codigo) VALUES (1, 'Administrador', 'Administrador del sistema', 'ADMIN') ON CONFLICT DO NOTHING;
INSERT INTO gestmusica.rol (id, nombre, descripcion, codigo) VALUES (2, 'Agencia', 'Usuario agencia', 'AGENCIA') ON CONFLICT DO NOTHING;
INSERT INTO gestmusica.rol (id, nombre, descripcion, codigo) VALUES (3, 'Artista', 'Usuario artista', 'ARTISTA') ON CONFLICT DO NOTHING;

-- Insert Usuarios de prueba
-- user1: Madrid + AGENCIA
INSERT INTO gestmusica.usuario (username, nombre, apellidos, password, email, activo, id_rol, provincia_id, email_baja, validado) 
VALUES ('user1', 'Usuario', 'Uno', 'pass', 'user1@test.com', true, 2, 1, false, true);

-- user2: Madrid + ARTISTA
INSERT INTO gestmusica.usuario (username, nombre, apellidos, password, email, activo, id_rol, provincia_id, email_baja, validado) 
VALUES ('user2', 'Usuario', 'Dos', 'pass', 'user2@test.com', true, 3, 1, false, true);

-- user3: Barcelona + AGENCIA
INSERT INTO gestmusica.usuario (username, nombre, apellidos, password, email, activo, id_rol, provincia_id, email_baja, validado) 
VALUES ('user3', 'Usuario', 'Tres', 'pass', 'user3@test.com', true, 2, 2, false, true);

-- user4: Barcelona + ADMIN
INSERT INTO gestmusica.usuario (username, nombre, apellidos, password, email, activo, id_rol, provincia_id, email_baja, validado) 
VALUES ('user4', 'Usuario', 'Cuatro', 'pass', 'user4@test.com', true, 1, 2, false, true);

-- user5: Inactivo (no debe aparecer)
INSERT INTO gestmusica.usuario (username, nombre, apellidos, password, email, activo, id_rol, provincia_id, email_baja, validado) 
VALUES ('user5', 'Usuario', 'Cinco', 'pass', 'user5@test.com', false, 2, 1, false, true);
