import { expect, type Page, test } from '@playwright/test';
import { MISSING_CREDENTIALS, e2eId, hasCredentials } from '../support/env';
import { alertWith, chooseFirstOption, choicesOptionValue, formSnapshot, postTo, selectedValues, toast } from '../support/ui';

/*
 * Create -> edit -> re-save an E2E- listado configuration (ajuste) and save its listado options
 * from /listado. The ajuste is never the default one ("Configuración predeterminada" off), so the
 * user's listados keep their settings. The UI cannot delete ajustes:
 * sql/1.7.1/01_e2e_cleanup_formularios.sql removes them.
 */
const FORM = '#formAjustes';

interface AjusteJson {
  nombre: string;
  idsAgencias: number[] | null;
  idsTipoArtista: number[] | null;
  idsComunidades: number[] | null;
  predeterminado: boolean;
}

async function ajusteJson(page: Page, id: string): Promise<AjusteJson> {
  const response = await page.request.get(`/ajustes/${id}/json`);
  expect(response.status()).toBe(200);
  return (await response.json()) as AjusteJson;
}

function sorted(ids: (number | string)[] | null | undefined): string[] {
  return (ids ?? []).map(String).sort();
}

async function saveAjuste(page: Page): Promise<void> {
  const responsePromise = postTo(page, '/ajustes/guardar');
  await page.locator(FORM).getByRole('button', { name: 'Guardar configuración' }).click();
  expect((await responsePromise).status()).toBe(302);
  await expect(page).toHaveURL(/\/ajustes$/);
  await expect(alertWith(page, 'Ajustes guardados correctamente')).toBeVisible();
}

test.describe.serial('Guardar ajustes', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);

  let idAjuste = '';
  let nombre = '';

  test('crea un ajuste E2E- no predeterminado', async ({ page }) => {
    nombre = e2eId('ajuste');
    await page.goto('/ajustes/nuevo');
    const form = page.locator(FORM);
    await form.locator('input[name="nombre"]').fill(nombre);
    await form.locator('#predeterminado').uncheck();
    await chooseFirstOption(page, 'ccaa');

    await saveAjuste(page);

    // /ajustes only lists the configurations when one is the default: find the new one in /listado.
    await page.goto('/listado');
    idAjuste = (await choicesOptionValue(page, 'selectAjustes', nombre)) ?? '';
    expect(idAjuste, `el ajuste ${nombre} no aparece en /listado`).not.toBe('');
    const json = await ajusteJson(page, idAjuste);
    expect(json).toMatchObject({ nombre, predeterminado: false });
    expect(sorted(json.idsComunidades)).toHaveLength(1);
  });

  test('edita el ajuste E2E-', async ({ page }) => {
    test.skip(idAjuste === '', 'No se creó el ajuste E2E- en la prueba anterior.');
    await page.goto(`/ajustes/${idAjuste}`);
    const form = page.locator(FORM);
    await expect(form.locator('input[name="nombre"]')).toHaveValue(nombre);

    nombre = `${nombre}-ed`;
    await form.locator('input[name="nombre"]').fill(nombre);
    await chooseFirstOption(page, 'tiposArtista');
    const tiposArtista = await selectedValues(page, 'tiposArtista');
    expect(tiposArtista).toHaveLength(1);

    await saveAjuste(page);

    await page.goto(`/ajustes/${idAjuste}`);
    await expect(page.locator(`${FORM} input[name="nombre"]`)).toHaveValue(nombre);
    expect(await selectedValues(page, 'tiposArtista')).toEqual(tiposArtista);
    expect(await ajusteJson(page, idAjuste)).toMatchObject({ nombre, predeterminado: false });
  });

  test('re-guarda el ajuste E2E- sin cambios', async ({ page }) => {
    test.skip(idAjuste === '', 'No se creó el ajuste E2E- en la prueba anterior.');
    await page.goto(`/ajustes/${idAjuste}`);
    const before = await formSnapshot(page.locator(FORM));
    expect(before).toContainEqual(['id', idAjuste]);

    await saveAjuste(page);

    await page.goto(`/ajustes/${idAjuste}`);
    expect(await formSnapshot(page.locator(FORM))).toEqual(before);
  });

  test('guarda las opciones de listado del ajuste E2E- sin cambios', async ({ page }) => {
    test.skip(idAjuste === '', 'No se creó el ajuste E2E- en la prueba anterior.');
    const before = await ajusteJson(page, idAjuste);

    await page.goto('/listado');
    const wrapper = page.locator('.choices').filter({ has: page.locator('#selectAjustes') });
    await wrapper.click();
    await wrapper.locator(`.choices__list--dropdown .choices__item--choice[data-value="${idAjuste}"]`).click();
    // Selecting it loads the ajuste into the listado filters.
    await expect(toast(page, 'success').filter({ hasText: 'Ajuste cargado correctamente' }).first()).toBeVisible();
    expect(sorted(await selectedValues(page, 'ccaa'))).toEqual(sorted(before.idsComunidades));

    const responsePromise = postTo(page, `/ajustes/${idAjuste}/opciones-listado`);
    await page.locator('#btn-guardar-configuracion-listado').click();
    const response = await responsePromise;
    expect(response.status()).toBe(200);
    expect(await response.json()).toMatchObject({ success: true, message: 'Configuración guardada correctamente' });
    await expect(toast(page, 'success').filter({ hasText: 'Configuración guardada correctamente' })).toBeVisible();

    const after = await ajusteJson(page, idAjuste);
    expect(sorted(after.idsAgencias)).toEqual(sorted(before.idsAgencias));
    expect(sorted(after.idsTipoArtista)).toEqual(sorted(before.idsTipoArtista));
    expect(sorted(after.idsComunidades)).toEqual(sorted(before.idsComunidades));
  });
});
