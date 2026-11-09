import { expect, type Page, test } from '@playwright/test';
import { BASE_URL, E2E_ARTISTA_ID, MISSING_ARTISTA, MISSING_CREDENTIALS, STORAGE_STATE, hasCredentials } from '../support/env';
import { addDays, formatDdMmYyyy, randomDayIn, setFlatpickrDate, toast } from '../support/ui';

/*
 * Create -> edit -> delete tarifas for a 3-day range on the artista page (/artista/{id}).
 * Saving a range upserts one tarifa per day; "Eliminar" deactivates them (activo = false).
 * The range falls in 2099, a year with no real tarifas, and the test refuses to run if any
 * day already has one, so it never overwrites real data.
 *
 * In the UI the "Eliminar" button only appears after clicking an existing tarifa in the
 * calendar, which cannot be reached in 2099 without paging hundreds of months. The test
 * reveals that same button in the open modal and goes through its confirmation dialog,
 * which runs the page's own eliminar_tarifas() with the selected range.
 */
const E2E_YEAR = 2099;
const DAYS = 3;

let pendingRange: string[] | null = null;

function rangeFrom(start: Date): string[] {
  return Array.from({ length: DAYS }, (_, i) => formatDdMmYyyy(addDays(start, i)));
}

async function importeEn(page: Page, fecha: string): Promise<string | null> {
  const response = await page.request.get(`/tarifa/${E2E_ARTISTA_ID}/${fecha}`);
  expect(response.status()).toBe(200);
  const text = await response.text();
  return text === '' ? null : (JSON.parse(text) as { title: string }).title;
}

async function openModal(page: Page, range: string[], importe: string): Promise<void> {
  await page.locator('#btnNuevaTarifa').click();
  await expect(page.locator('#modalNuevaTarifa')).toBeVisible();
  await setFlatpickrDate(page, 'idFechaDesde', range[0]);
  await setFlatpickrDate(page, 'idFechaHasta', range[range.length - 1]);
  await page.locator('#importe').fill(importe);
}

async function saveRange(page: Page, range: string[], importe: string): Promise<void> {
  await openModal(page, range, importe);
  const responsePromise = page.waitForResponse((r) => r.url().endsWith('/tarifa/save'));
  await page.locator('#btn-guardar-tarifa').click();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({ success: true, message: 'Tarifa guardada' });
  await expect(toast(page, 'success').filter({ hasText: 'Tarifa guardada' }).first()).toBeVisible();
  await expect(page.locator('#modalNuevaTarifa')).toBeHidden();
}

async function deleteRange(page: Page, range: string[]): Promise<void> {
  await openModal(page, range, '0');
  await page.locator('#modal-tarifa-eliminar').evaluate((el) => {
    (el as HTMLElement).style.display = '';
  });
  await page.locator('#modal-tarifa-eliminar').click();
  await expect(page.locator('#modalConfirmacion')).toBeVisible();
  const responsePromise = page.waitForResponse((r) => r.url().endsWith('/tarifa/eliminar'));
  await page.locator('#btnConfirmar').click();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({ success: true, message: 'Tarifa eliminada' });
}

test.describe('Tarifas', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(E2E_ARTISTA_ID === '', MISSING_ARTISTA);

  test.afterAll(async ({ browser }) => {
    if (pendingRange === null) {
      return;
    }
    // A step failed after saving: deactivate the range through the UI.
    const context = await browser.newContext({ storageState: STORAGE_STATE, baseURL: BASE_URL });
    const page = await context.newPage();
    try {
      await page.goto(`/artista/${E2E_ARTISTA_ID}`);
      await deleteRange(page, pendingRange);
    } finally {
      await context.close();
    }
  });

  test('crea, edita y elimina tarifas de un rango de fechas', async ({ page }) => {
    const range = rangeFrom(randomDayIn(E2E_YEAR, 360));

    await page.goto(`/artista/${E2E_ARTISTA_ID}`);
    test.skip(
      (await page.locator('#btnNuevaTarifa').count()) === 0,
      `El usuario E2E no tiene permiso CREAR_TARIFAS sobre el artista ${E2E_ARTISTA_ID}.`,
    );
    for (const fecha of range) {
      expect(await importeEn(page, fecha), `ya existe una tarifa el ${fecha}; no se sobrescribe`).toBeNull();
    }

    await test.step('crear', async () => {
      pendingRange = range;
      await saveRange(page, range, '1111');
      for (const fecha of range) {
        expect(await importeEn(page, fecha)).toBe('1111');
      }
      // The day after the range is untouched.
      expect(await importeEn(page, formatDdMmYyyy(addDays(parseDdMmYyyy(range[DAYS - 1]), 1)))).toBeNull();
    });

    await test.step('editar', async () => {
      await saveRange(page, range, '2222');
      for (const fecha of range) {
        expect(await importeEn(page, fecha)).toBe('2222');
      }
    });

    await test.step('eliminar', async () => {
      await deleteRange(page, range);
      pendingRange = null;
      for (const fecha of range) {
        expect(await importeEn(page, fecha)).toBeNull();
      }
    });
  });
});

function parseDdMmYyyy(value: string): Date {
  const [dd, mm, yyyy] = value.split('-').map(Number);
  return new Date(Date.UTC(yyyy, mm - 1, dd));
}
