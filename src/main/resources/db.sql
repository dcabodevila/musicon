--
-- PostgreSQL database dump
--

-- Dumped from database version 15.1
-- Dumped by pg_dump version 15.1

-- Started on 2023-01-05 16:53:35

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 6 (class 2615 OID 16399)
-- Name: musicon; Type: SCHEMA; Schema: -; Owner: postgres
--

CREATE SCHEMA musicon;


ALTER SCHEMA musicon OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 219 (class 1259 OID 24602)
-- Name: permiso; Type: TABLE; Schema: musicon; Owner: postgres
--

CREATE TABLE musicon.permiso (
    id bigint NOT NULL,
    codigo character varying(255) NOT NULL,
    descripcion character varying(255)
);


ALTER TABLE musicon.permiso OWNER TO postgres;

--
-- TOC entry 220 (class 1259 OID 24605)
-- Name: permiso_ID_seq; Type: SEQUENCE; Schema: musicon; Owner: postgres
--

ALTER TABLE musicon.permiso ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME musicon."permiso_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 218 (class 1259 OID 24595)
-- Name: rol; Type: TABLE; Schema: musicon; Owner: postgres
--

CREATE TABLE musicon.rol (
    id bigint NOT NULL,
    nombre character varying(255) NOT NULL,
    descripcion character varying(255)
);


ALTER TABLE musicon.rol OWNER TO postgres;

--
-- TOC entry 217 (class 1259 OID 24594)
-- Name: rol_ID_seq; Type: SEQUENCE; Schema: musicon; Owner: postgres
--

ALTER TABLE musicon.rol ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME musicon."rol_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 221 (class 1259 OID 24613)
-- Name: rol_permisos; Type: TABLE; Schema: musicon; Owner: postgres
--

CREATE TABLE musicon.rol_permisos (
    rol_id bigint NOT NULL,
    permiso_id bigint NOT NULL
);


ALTER TABLE musicon.rol_permisos OWNER TO postgres;

--
-- TOC entry 215 (class 1259 OID 16400)
-- Name: usuario; Type: TABLE; Schema: musicon; Owner: postgres
--

CREATE TABLE musicon.usuario (
    id bigint NOT NULL,
    nombre character varying(255) NOT NULL,
    apellidos character varying(255),
    apodo character varying(255),
    pass character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    activate_key character varying(255),
    fecha_ultimo_acceso timestamp with time zone,
    fecha_registro timestamp with time zone,
    activo boolean NOT NULL,
    recover character varying(255),
    username character varying(255) NOT NULL,
    id_rol bigint
);


ALTER TABLE musicon.usuario OWNER TO postgres;

--
-- TOC entry 216 (class 1259 OID 16403)
-- Name: usuario_ID_seq; Type: SEQUENCE; Schema: musicon; Owner: postgres
--

ALTER TABLE musicon.usuario ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME musicon."usuario_ID_seq"
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 3341 (class 0 OID 24602)
-- Dependencies: 219
-- Data for Name: permiso; Type: TABLE DATA; Schema: musicon; Owner: postgres
--

INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (1, 'ACCESO_PANEL_ADMIN', 'Acceso al panel de administración');
INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (2, 'GESTION_AGRUPACION', 'Gestionar agrupaciones');
INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (3, 'GESTION_TARIFAS', 'Gestionar tarifas');
INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (4, 'OCUPACIONES', 'Realizar ocupaciones');
INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (5, 'RESERVAS', 'Realizar reservas');
INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (6, 'LISTADOS', 'Obtener listados');
INSERT INTO musicon.permiso (id, codigo, descripcion) OVERRIDING SYSTEM VALUE VALUES (7, 'USUARIOS', 'Acceso a la gestión de usuarios');


--
-- TOC entry 3340 (class 0 OID 24595)
-- Dependencies: 218
-- Data for Name: rol; Type: TABLE DATA; Schema: musicon; Owner: postgres
--

INSERT INTO musicon.rol (id, nombre, descripcion) OVERRIDING SYSTEM VALUE VALUES (1, 'Administrador', 'Administrador de la aplicación');
INSERT INTO musicon.rol (id, nombre, descripcion) OVERRIDING SYSTEM VALUE VALUES (2, 'Representante', 'Representante de artistas');
INSERT INTO musicon.rol (id, nombre, descripcion) OVERRIDING SYSTEM VALUE VALUES (3, 'Agente', 'Agente');


--
-- TOC entry 3343 (class 0 OID 24613)
-- Dependencies: 221
-- Data for Name: rol_permisos; Type: TABLE DATA; Schema: musicon; Owner: postgres
--

INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 1);
INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 2);
INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 3);
INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 4);
INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 5);
INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 6);
INSERT INTO musicon.rol_permisos (rol_id, permiso_id) VALUES (1, 7);


--
-- TOC entry 3337 (class 0 OID 16400)
-- Dependencies: 215
-- Data for Name: usuario; Type: TABLE DATA; Schema: musicon; Owner: postgres
--

INSERT INTO musicon.usuario (id, nombre, apellidos, apodo, pass, email, activate_key, fecha_ultimo_acceso, fecha_registro, activo, recover, username, id_rol) OVERRIDING SYSTEM VALUE VALUES (25, 'Sara', 'Martínez Gutiérrez', NULL, '$2a$10$NKNj7dme163v0veNMMe72OXIl0VJUPJapdqIR2RxlcDKclDuEyhqW', 'saramagu232131@gmail.com', '4e277c95-73b7-4ffa-bd4a-c16922e705c9', '2023-01-04 15:00:37.153+01', '2023-01-04 15:00:23.628+01', false, NULL, 'saramagu', NULL);
INSERT INTO musicon.usuario (id, nombre, apellidos, apodo, pass, email, activate_key, fecha_ultimo_acceso, fecha_registro, activo, recover, username, id_rol) OVERRIDING SYSTEM VALUE VALUES (24, 'David', 'Cabodevila Gasalla', NULL, '$2a$10$z.RyL8bsHH.PS/qADq.wAeOXAH5xsc8akuMZNkZoEc1xIIqtoeyNS', 'dcabodevila@gmail.com', '16439bf4-7dfc-4b7f-b582-1e21e91f62bd', '2023-01-04 19:44:32.999+01', '2023-01-04 13:21:43.824+01', false, NULL, 'dcabodevila', 1);
INSERT INTO musicon.usuario (id, nombre, apellidos, apodo, pass, email, activate_key, fecha_ultimo_acceso, fecha_registro, activo, recover, username, id_rol) OVERRIDING SYSTEM VALUE VALUES (21, 'Nombre', 'Apellidos Apellidos-González', NULL, '$2a$10$dSa0kKuCqIPiZA7hs2sATeiUELobDyt1Hh4a.CdOhkNMtYMgmPmfe', 'dcabodevila1234@gmail.com', 'd3742ac7-dca4-40d9-9104-d79a99bd255a', '2023-01-03 22:28:35.484+01', '2023-01-03 22:27:53.788+01', false, NULL, 'prueba', NULL);
INSERT INTO musicon.usuario (id, nombre, apellidos, apodo, pass, email, activate_key, fecha_ultimo_acceso, fecha_registro, activo, recover, username, id_rol) OVERRIDING SYSTEM VALUE VALUES (22, 'asddas', ' ', NULL, '$2a$10$rv3NlRGhFJABhjYfbcrp6O8BVW0E6znwFNWNBa0UY2e9uEGg.Ln3u', 'dcabodevila@gmail.com', '9f57f6d8-bdc8-441b-91c5-af8a1728082d', '2023-01-03 22:33:41.714+01', '2023-01-03 22:33:41.704+01', false, NULL, 'test', NULL);
INSERT INTO musicon.usuario (id, nombre, apellidos, apodo, pass, email, activate_key, fecha_ultimo_acceso, fecha_registro, activo, recover, username, id_rol) OVERRIDING SYSTEM VALUE VALUES (23, 'asd', 'asdas', NULL, '$2a$10$lahkWi16F0PLhNDP24c2E.94ktb.xLnwGRv03ocvE1iXbrM.hZ1ai', 'asdsad@asdsa.com', '674371cf-d84a-4cec-a2c9-502404f6133f', NULL, '2023-01-03 22:34:06.381+01', false, NULL, 'test', NULL);


--
-- TOC entry 3349 (class 0 OID 0)
-- Dependencies: 220
-- Name: permiso_ID_seq; Type: SEQUENCE SET; Schema: musicon; Owner: postgres
--

SELECT pg_catalog.setval('musicon."permiso_ID_seq"', 7, true);


--
-- TOC entry 3350 (class 0 OID 0)
-- Dependencies: 217
-- Name: rol_ID_seq; Type: SEQUENCE SET; Schema: musicon; Owner: postgres
--

SELECT pg_catalog.setval('musicon."rol_ID_seq"', 3, true);


--
-- TOC entry 3351 (class 0 OID 0)
-- Dependencies: 216
-- Name: usuario_ID_seq; Type: SEQUENCE SET; Schema: musicon; Owner: postgres
--

SELECT pg_catalog.setval('musicon."usuario_ID_seq"', 25, true);


--
-- TOC entry 3191 (class 2606 OID 24610)
-- Name: permiso permiso_pkey; Type: CONSTRAINT; Schema: musicon; Owner: postgres
--

ALTER TABLE ONLY musicon.permiso
    ADD CONSTRAINT permiso_pkey PRIMARY KEY (id);


--
-- TOC entry 3189 (class 2606 OID 24599)
-- Name: rol rol_pkey; Type: CONSTRAINT; Schema: musicon; Owner: postgres
--

ALTER TABLE ONLY musicon.rol
    ADD CONSTRAINT rol_pkey PRIMARY KEY (id);


--
-- TOC entry 3193 (class 2606 OID 24621)
-- Name: rol_permisos rol_permisos_permiso_id_fkey; Type: FK CONSTRAINT; Schema: musicon; Owner: postgres
--

ALTER TABLE ONLY musicon.rol_permisos
    ADD CONSTRAINT rol_permisos_permiso_id_fkey FOREIGN KEY (permiso_id) REFERENCES musicon.permiso(id) NOT VALID;


--
-- TOC entry 3194 (class 2606 OID 24616)
-- Name: rol_permisos rol_permisos_rol_id_fkey; Type: FK CONSTRAINT; Schema: musicon; Owner: postgres
--

ALTER TABLE ONLY musicon.rol_permisos
    ADD CONSTRAINT rol_permisos_rol_id_fkey FOREIGN KEY (rol_id) REFERENCES musicon.rol(id) NOT VALID;


--
-- TOC entry 3192 (class 2606 OID 24626)
-- Name: usuario usuario_id_rol_fkey; Type: FK CONSTRAINT; Schema: musicon; Owner: postgres
--

ALTER TABLE ONLY musicon.usuario
    ADD CONSTRAINT usuario_id_rol_fkey FOREIGN KEY (id_rol) REFERENCES musicon.rol(id) NOT VALID;


-- Completed on 2023-01-05 16:53:36

--
-- PostgreSQL database dump complete
--

