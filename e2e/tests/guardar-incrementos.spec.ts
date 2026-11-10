import { expect, type Page, test } from '@playwright/test';
import { E2E_ARTISTA_ID, MISSING_ARTISTA, MISSING_CREDENTIALS, hasCredentials } from '../support/env';
import { postTo, toast } from '../support/ui';

/*
 * Saves a provincial increment ("Inc. provinciales" on /artista/{id}) and re-saves it unchanged.
 * There is one increment per artista and provincia and the UI cannot delete them, so the test
 * picks a provincia without increment (or one left at 0 by a previous run) and saves an amount of
 * 0, which prices the same as having none. It never overwrites a non-zero increment. sql/1.7.1/01_e2e_cleanup_formularios.sql has an
 * optional, commented delete for these rows.
 */
interface IncrementoJson {
  descripcionProvincia: string;
  incremento: number;
}

async function incrementos(page: Page): Promise<IncrementoJson[]> {
  const response = await page.request.get(`/incremento/list/${E2E_ARTISTA_ID}`);
  expect(response.status()).toBe(200);
  return (await response.json()) as IncrementoJson[];
}

async function guardar(page: Page): Promise<void> {
  const responsePromise = postTo(page, '/incremento/save');
  await page.locator('#btn-guardar-incremento').click();
  const response = await responsePromise;
  expect(response.status()).toBe(200);
  expect(await response.json()).toMatchObject({ success: true, message: 'Incremento guardado' });
  await expect(toast(page, 'success').filter({ hasText: 'Incremento guardado' }).first()).toBeVisible();
}

test.describe('Guardar incrementos', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(E2E_ARTISTA_ID === '', MISSING_ARTISTA);

  test('guarda un incremento provincial de 0 y lo re-guarda sin cambios', async ({ page }) => {
    await page.goto(`/artista/${E2E_ARTISTA_ID}`);
    const abrir = page.locator('#btn-incrementos');
    test.skip((await abrir.count()) === 0, `El usuario E2E no tiene permiso CREAR_TARIFAS sobre el artista ${E2E_ARTISTA_ID}.`);

    const actuales = await incrementos(page);
    const existentes = actuales.map((i) => i.descripcionProvincia);
    const aCero = actuales.filter((i) => Number(i.incremento) === 0).map((i) => i.descripcionProvincia);
    const modal = page.locator('#modalIncrementos');
    const opciones = await modal.locator('#provincia option').evaluateAll((options) =>
      options.map((o) => ({ value: (o as HTMLOptionElement).value, text: (o.textContent ?? '').trim() })),
    );
    const provincia = opciones.find((o) => aCero.includes(o.text)) ?? opciones.find((o) => !existentes.includes(o.text));
    test.skip(!provincia, `Todas las provincias tienen ya incremento en el artista ${E2E_ARTISTA_ID}; no se sobrescriben.`);

    await abrir.click();
    await expect(modal).toBeVisible();
    await modal.locator('#provincia').selectOption(provincia!.value);
    await modal.locator('#incremento').fill('0');
    const tipo = (await modal.locator('#tipoIncremento option:checked').textContent())?.trim() ?? '';

    await test.step('crear', async () => {
      await guardar(page);
      const fila = modal.locator('#tablaIncrementos tbody tr').filter({ hasText: provincia!.text });
      await expect(fila).toHaveCount(1);
      await expect(fila.locator('td')).toHaveText([provincia!.text, tipo, '0']);
    });

    await test.step('re-guardar sin cambios', async () => {
      await guardar(page);
      const guardados = (await incrementos(page)).filter((i) => i.descripcionProvincia === provincia!.text);
      expect(guardados).toHaveLength(1);
      expect(Number(guardados[0].incremento)).toBe(0);
      await expect(modal.locator('#tablaIncrementos tbody tr').filter({ hasText: provincia!.text })).toHaveCount(1);
    });
  });
});
