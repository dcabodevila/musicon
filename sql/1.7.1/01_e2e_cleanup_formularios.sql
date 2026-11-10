-- =====================================================================
-- Limpieza manual de los datos creados por los specs e2e de guardado de
-- formularios (e2e/tests/guardar-*.spec.ts)
-- =====================================================================
-- Ejecución MANUAL y opcional, nunca automática. Complementa a
-- sql/1.7.0/02_e2e_cleanup.sql y borra solo filas identificadas por el
-- prefijo "E2E-" que la suite pone en los textos que crea:
--   * artistas "E2E-..." (guardar-artista): la UI no los borra. El servidor
--     capitaliza el nombre ("E2e-artista-..."), por eso se compara con ILIKE.
--     Se borran con sus accesos, tipos, comunidades, contacto y el mensaje
--     interno "Nuevo artista en festia" que se envía a todos los usuarios.
--   * agencias "E2E-..." (guardar-agencia, solo si el usuario E2E tiene
--     AGENCIA_CREAR) sin artistas que no sean E2E, con sus accesos y contacto.
--   * ajustes "E2E-..." (guardar-ajustes): la UI no los borra.
--   * documentos "E2E-..." (guardar-documentos): la UI solo los desactiva.
--     El fichero subido a Cloudinary NO se borra desde aquí.
-- Revisa los SELECT de control antes del COMMIT.
-- =====================================================================

BEGIN;

-- Control: qué se va a borrar
SELECT id, nombre, agencia_id, usuario_id, activo
FROM gestmusica.artista
WHERE nombre ILIKE 'E2E-%';

SELECT a.id, a.nombre, a.usuario_id, a.activo
FROM gestmusica.agencia a
WHERE a.nombre LIKE 'E2E-%'
  AND NOT EXISTS (SELECT 1 FROM gestmusica.artista ar
                  WHERE ar.agencia_id = a.id AND ar.nombre NOT ILIKE 'E2E-%');

SELECT id, nombre, usuario_id, predeterminado
FROM gestmusica.ajustes
WHERE nombre LIKE 'E2E-%';

SELECT id, nombre, artista_id, activo, fecha_creacion
FROM gestmusica.documento
WHERE nombre LIKE 'E2E-%';

-- Artistas E2E y sus dependencias
CREATE TEMP TABLE e2e_artista ON COMMIT DROP AS
SELECT id, contacto_id
FROM gestmusica.artista
WHERE nombre ILIKE 'E2E-%';

DELETE FROM gestmusica.mensaje m
USING e2e_artista a
WHERE m.url_enlace = '/artista/' || a.id;

DELETE FROM gestmusica.mensaje m
USING gestmusica.ocupacion oc, e2e_artista a
WHERE oc.artista_id = a.id
  AND m.url_enlace = '/ocupacion/' || oc.id;

DELETE FROM gestmusica.odg_sincronizacion_tracking t
USING e2e_artista a
WHERE t.artista_id = a.id;

DELETE FROM gestmusica.ocupacion oc
USING e2e_artista a
WHERE oc.artista_id = a.id;

DELETE FROM gestmusica.tarifa t
USING e2e_artista a
WHERE t.artista_id = a.id;

DELETE FROM gestmusica.incremento_provincial i
USING e2e_artista a
WHERE i.artista_id = a.id;

DELETE FROM gestmusica.documento d
USING e2e_artista a
WHERE d.artista_id = a.id;

DELETE FROM gestmusica.acceso_artista aa
USING e2e_artista a
WHERE aa.artista_id = a.id;

DELETE FROM gestmusica.acceso ac
USING e2e_artista a
WHERE ac.artista_id = a.id;

DELETE FROM gestmusica.artista_tipo_artista ata
USING e2e_artista a
WHERE ata.artista_id = a.id;

DELETE FROM gestmusica.artista_ccaa ac
USING e2e_artista a
WHERE ac.artista_id = a.id;

DELETE FROM gestmusica.artista ar
USING e2e_artista a
WHERE ar.id = a.id;

DELETE FROM gestmusica.contacto c
USING e2e_artista a
WHERE c.id = a.contacto_id;

-- Agencias E2E (solo las que ya no tienen artistas tras el bloque anterior)
CREATE TEMP TABLE e2e_agencia ON COMMIT DROP AS
SELECT a.id, a.agencia_contacto_id
FROM gestmusica.agencia a
WHERE a.nombre LIKE 'E2E-%'
  AND NOT EXISTS (SELECT 1 FROM gestmusica.artista ar WHERE ar.agencia_id = a.id);

DELETE FROM gestmusica.acceso ac
USING e2e_agencia a
WHERE ac.agencia_id = a.id;

DELETE FROM gestmusica.agencia_decision d
USING e2e_agencia a
WHERE d.agencia_id = a.id;

DELETE FROM gestmusica.ajustes_agencias aa
USING e2e_agencia a
WHERE aa.agencia_id = a.id;

DELETE FROM gestmusica.listado_agencia la
USING e2e_agencia a
WHERE la.agencia_id = a.id;

DELETE FROM gestmusica.agencia ag
USING e2e_agencia a
WHERE ag.id = a.id;

DELETE FROM gestmusica.contacto c
USING e2e_agencia a
WHERE c.id = a.agencia_contacto_id;

-- Ajustes E2E
DELETE FROM gestmusica.ajustes_tipo_artista x
USING gestmusica.ajustes a
WHERE x.ajuste_id = a.id AND a.nombre LIKE 'E2E-%';

DELETE FROM gestmusica.ajustes_agencias x
USING gestmusica.ajustes a
WHERE x.ajuste_id = a.id AND a.nombre LIKE 'E2E-%';

DELETE FROM gestmusica.ajustes_comunidades x
USING gestmusica.ajustes a
WHERE x.ajuste_id = a.id AND a.nombre LIKE 'E2E-%';

DELETE FROM gestmusica.ajustes
WHERE nombre LIKE 'E2E-%';

-- Documentos E2E de artistas reales (desactivados por la UI)
DELETE FROM gestmusica.documento
WHERE nombre LIKE 'E2E-%';

-- Los incrementos provinciales del spec guardar-incrementos no llevan texto
-- propio: la suite guarda importe 0 (equivale a no tener incremento) en una
-- provincia sin incremento del artista E2E_ARTISTA_ID. Si se quieren eliminar,
-- revisa antes el artista usado y ejecuta a mano, por ejemplo:
-- DELETE FROM gestmusica.incremento_provincial
-- WHERE artista_id = <E2E_ARTISTA_ID> AND incremento = 0;

COMMIT;
