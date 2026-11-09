-- =====================================================================
-- Limpieza manual de los datos creados por la suite Playwright (e2e/)
-- =====================================================================
-- Ejecución MANUAL y opcional, nunca automática. Borra solo filas
-- identificadas por el prefijo "E2E-" que la suite pone en los textos que crea:
--   * ocupaciones anuladas (lugar/poblacion "E2E-..."): la UI solo las anula,
--     no las borra; junto con sus mensajes internos. Sus tarifas NO se borran:
--     una ocupación E2E puede usar una tarifa real y no hay forma fiable de
--     saber si la creó la suite.
--   * listados generados (solicitado_para "E2E-..."): la UI no los borra.
-- Revisa los SELECT de control antes del COMMIT.
-- =====================================================================

BEGIN;

-- Control: qué se va a borrar
SELECT id, artista_id, fecha, lugar, poblacion, estado_id
FROM gestmusica.ocupacion
WHERE lugar LIKE 'E2E-%' OR poblacion LIKE 'E2E-%';

SELECT id, solicitado_para, fecha_creacion
FROM gestmusica.listado
WHERE solicitado_para LIKE 'E2E-%';

-- Control: tarifas usadas por ocupaciones E2E (se conservan; revísalas a mano)
SELECT t.id, t.artista_id, t.fecha, t.importe, t.activo
FROM gestmusica.tarifa t
WHERE t.id IN (SELECT tarifa_id FROM gestmusica.ocupacion
               WHERE (lugar LIKE 'E2E-%' OR poblacion LIKE 'E2E-%') AND tarifa_id IS NOT NULL);

-- Ocupaciones E2E y sus dependencias
CREATE TEMP TABLE e2e_ocupacion ON COMMIT DROP AS
SELECT id, tarifa_id
FROM gestmusica.ocupacion
WHERE lugar LIKE 'E2E-%' OR poblacion LIKE 'E2E-%';

DELETE FROM gestmusica.mensaje m
USING e2e_ocupacion o
WHERE m.url_enlace = '/ocupacion/' || o.id;

DELETE FROM gestmusica.odg_sincronizacion_tracking t
USING e2e_ocupacion o
WHERE t.ocupacion_id = o.id;

DELETE FROM gestmusica.ocupacion oc
USING e2e_ocupacion o
WHERE oc.id = o.id;

-- Listados E2E
DELETE FROM gestmusica.listado_agencia la
USING gestmusica.listado l
WHERE la.listado_id = l.id
  AND l.solicitado_para LIKE 'E2E-%';

DELETE FROM gestmusica.listado
WHERE solicitado_para LIKE 'E2E-%';

-- Las tarifas del spec de tarifas no llevan texto propio: la UI las desactiva
-- (activo = false) en fechas de 2099 y no se borran aquí. Si se quieren eliminar,
-- revisa antes el artista usado en E2E_ARTISTA_ID y ejecuta a mano, por ejemplo:
-- DELETE FROM gestmusica.tarifa
-- WHERE artista_id = <E2E_ARTISTA_ID> AND activo = false
--   AND fecha >= '2099-01-01' AND fecha < '2100-01-01'
--   AND NOT EXISTS (SELECT 1 FROM gestmusica.ocupacion oc WHERE oc.tarifa_id = gestmusica.tarifa.id);

COMMIT;
