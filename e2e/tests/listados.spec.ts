import { expect, test } from '@playwright/test';
import { MISSING_CREDENTIALS, e2eId, hasCredentials } from '../support/env';
import { addDays, chooseFirstOption, formatDdMmYyyy, selectedValues, setFlatpickrDate, toast } from '../support/ui';

/*
 * Each generated listado is stored in gestmusica.listado with the "Solicitado para" text.
 * The UI cannot delete listados, so the suite uses an E2E- prefix there and
 * sql/1.7.0/02_e2e_cleanup.sql removes them manually.
 */
test.describe('Listados', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);

  test.beforeEach(async ({ page }) => {
    await page.goto('/listado');
    await expect(page.locator('#formGenerarListado')).toBeVisible();
  });

  test('sin fechas no se genera el listado y se avisa al usuario', async ({ page }) => {
    let generated = false;
    page.on('request', (request) => {
      if (request.url().includes('/listado/generar')) {
        generated = true;
      }
    });

    await page.locator('#btn-generar-listado').click();

    await expect(toast(page, 'error')).toHaveText('Introduce fecha inicial y final, o al menos una fecha suelta.');
    expect(generated).toBe(false);
  });

  test('genera un listado con filtros y descarga el PDF', async ({ page }) => {
    const solicitadoPara = e2eId('listado');
    await page.locator('#solicitado-listado').fill(solicitadoPara);
    await setFlatpickrDate(page, 'idFecha1', formatDdMmYyyy(addDays(new Date(), 45)));

    // Required filters: keep the user's saved settings, otherwise pick the first option.
    for (const selectId of ['agencias', 'tiposArtista', 'ccaa']) {
      if ((await selectedValues(page, selectId)).length === 0) {
        await chooseFirstOption(page, selectId);
      }
      expect(await selectedValues(page, selectId)).not.toHaveLength(0);
    }

    const responsePromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().endsWith('/listado/generar'),
    );
    await page.locator('#btn-generar-listado').click();
    const response = await responsePromise;

    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('application/pdf');
    expect(response.headers()['content-disposition']).toMatch(/attachment; filename="?Listado_.*\.pdf/);
    const body = await response.body();
    expect(body.subarray(0, 5).toString('latin1')).toBe('%PDF-');

    const requestBody = response.request().postData() ?? '';
    expect(new URLSearchParams(requestBody).get('solicitadoPara')).toBe(solicitadoPara);

    await expect(page.locator('.notyf__toast--success')).toContainText('Ver PDF');
    await expect(page.locator('#btn-generar-listado')).toBeEnabled();
  });
});

