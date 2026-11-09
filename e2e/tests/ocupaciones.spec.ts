import { expect, type Page, test } from '@playwright/test';
import {
  BASE_URL,
  E2E_ARTISTA_ID,
  MISSING_ARTISTA,
  MISSING_CREDENTIALS,
  STORAGE_STATE,
  e2eId,
  hasCredentials,
} from '../support/env';
import { formatDdMmYyyy, okToast, randomDayIn, setFlatpickrDate } from '../support/ui';

/*
 * Create -> edit -> anular on /ocupacion/nueva/{idArtista} and /ocupacion/{id}.
 * The UI has no hard delete: "Anular" only changes the state, so the anulada row (and the
 * tarifa it created on that date) stay in the database. They carry the E2E- prefix in
 * "lugar"/"localidad" and sql/1.7.0/02_e2e_cleanup.sql removes them manually.
 * Dates fall in 2098 so they never collide with real bookings.
 */
const E2E_YEAR = 2098;

interface SaveResponse {
  success: boolean;
  message: string;
  messageType: string;
  idEntidad: number | null;
}

interface OcupacionEdit {
  id: number;
  start: string;
  idArtista: number;
  lugar: string;
  localidad: string;
  estado: string;
}

let ocupacionId: number | null = null;
let anulada = false;

async function getOcupacion(page: Page, id: number): Promise<OcupacionEdit> {
  const response = await page.request.get(`/ocupacion/get/${id}`);
  expect(response.status()).toBe(200);
  return (await response.json()) as OcupacionEdit;
}

async function openOcupacion(page: Page, id: number, lugar: string): Promise<void> {
  await page.goto(`/ocupacion/${id}`);
  await expect(page.locator('#id-ocupacion')).toHaveValue(String(id));
  // obtenerOcupacionDto() fills the form asynchronously; wait until it is done.
  await expect(page.locator('#lugar-ocupacion')).toHaveValue(lugar);
}

async function saveForm(page: Page): Promise<SaveResponse> {
  const responsePromise = page.waitForResponse(
    (r) => r.request().method() === 'POST' && r.url().endsWith('/ocupacion/save'),
  );
  await page.locator('#btn-guardar-ocupacion').click();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  return (await response.json()) as SaveResponse;
}

async function anular(page: Page, id: number): Promise<{ success: boolean; message: string }> {
  // The page reloads right after the (synchronous) XHR, which can discard the response
  // body; capture it while proxying the real request instead. jQuery (`cache: false`) adds
  // a `?_=<timestamp>` query, so match on the path: a glob without the query never matches.
  const isAnular = (url: URL): boolean => url.pathname === `/ocupacion/anular/${id}`;
  let resolveBody: (value: { status: number; json: { success: boolean; message: string } }) => void;
  const captured = new Promise<{ status: number; json: { success: boolean; message: string } }>((resolve) => {
    resolveBody = resolve;
  });
  await page.route(isAnular, async (route) => {
    const response = await route.fetch();
    resolveBody({ status: response.status(), json: await response.json() });
    await route.fulfill({ response });
  });

  await page.locator('#btn-anular-ocupacion').click();
  await expect(page.locator('#modalConfirmacion')).toBeVisible();
  await page.locator('#btnConfirmar').click();
  const { status, json } = await captured;
  await page.unroute(isAnular);
  expect(status).toBe(200);
  return json;
}

test.describe('Ocupaciones', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(E2E_ARTISTA_ID === '', MISSING_ARTISTA);

  test.afterAll(async ({ browser }) => {
    if (ocupacionId === null || anulada) {
      return;
    }
    // A step failed after creating the ocupación: anular it through the UI.
    const context = await browser.newContext({ storageState: STORAGE_STATE, baseURL: BASE_URL });
    const page = await context.newPage();
    try {
      await page.goto(`/ocupacion/${ocupacionId}`);
      await expect(page.locator('#id-ocupacion')).toHaveValue(String(ocupacionId));
      await anular(page, ocupacionId);
    } finally {
      await context.close();
    }
  });

  test('crea, edita y anula una ocupación desde la interfaz', async ({ page }) => {
    const lugar = e2eId('ocupacion');
    const lugarEditado = `${lugar}-editada`;
    const fecha = randomDayIn(E2E_YEAR);
    const fechaIso = fecha.toISOString().slice(0, 10);

    await test.step('crear', async () => {
      const response = await page.goto(`/ocupacion/nueva/${E2E_ARTISTA_ID}`);
      test.skip(response?.status() === 403, `El usuario E2E no tiene permiso OCUPACIONES sobre el artista ${E2E_ARTISTA_ID}.`);
      await expect(page.locator('#formNuevaOcupacion')).toBeVisible();
      await expect(page.locator('#id-ocupacion')).toHaveValue('');

      await setFlatpickrDate(page, 'idFechaOcupacion', formatDdMmYyyy(fecha));
      await page.locator('#localidad-ocupacion').fill(lugar);
      await page.locator('#lugar-ocupacion').fill(lugar);
      await page.locator('#observaciones-ocupacion').fill(`${lugar} creada por la suite e2e`);

      const body = await saveForm(page);
      expect(body.success, body.message).toBe(true);
      expect(body.message).toMatch(/^Ocupacion guardada correctamente/);
      expect(body.idEntidad).toEqual(expect.any(Number));
      ocupacionId = body.idEntidad as number;
      await expect(okToast(page)).toContainText('Ocupacion guardada correctamente');
      await expect(page.locator('#id-ocupacion')).toHaveValue(String(ocupacionId));

      const ocupacion = await getOcupacion(page, ocupacionId);
      expect(ocupacion.idArtista).toBe(Number(E2E_ARTISTA_ID));
      expect(ocupacion.lugar).toBe(lugar);
      expect(ocupacion.localidad).toBe(lugar);
      expect(ocupacion.start.startsWith(fechaIso)).toBe(true);
      expect(ocupacion.estado).not.toBe('Anulado');
    });

    await test.step('editar', async () => {
      const id = ocupacionId as number;
      await openOcupacion(page, id, lugar);
      await page.locator('#lugar-ocupacion').fill(lugarEditado);

      const body = await saveForm(page);
      expect(body.success, body.message).toBe(true);
      expect(body.idEntidad).toBe(id);

      const ocupacion = await getOcupacion(page, id);
      expect(ocupacion.lugar).toBe(lugarEditado);
      expect(ocupacion.start.startsWith(fechaIso)).toBe(true);
    });

    await test.step('anular', async () => {
      const id = ocupacionId as number;
      await openOcupacion(page, id, lugarEditado);
      test.skip(
        (await page.locator('#btn-anular-ocupacion').count()) === 0,
        `El usuario E2E no tiene permiso ANULAR_OCUPACION sobre el artista ${E2E_ARTISTA_ID}; anúlala a mano o ejecuta el script de limpieza.`,
      );

      const body = await anular(page, id);
      expect(body.success, body.message).toBe(true);
      expect(body.message).toMatch(/^Ocupacion anulada correctamente/);
      anulada = true;

      expect((await getOcupacion(page, id)).estado).toBe('Anulado');
      await openOcupacion(page, id, lugarEditado);
      await expect(page.locator('#badge-estado-ocupacion')).toHaveText('Anulado');
    });
  });
});
