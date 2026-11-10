import { expect, test } from '@playwright/test';
import { MISSING_CREDENTIALS, e2eId, hasCredentials } from '../support/env';
import { alertWith, chooseFirstOption, chooseOptionByValue, formSnapshot, miUsuarioId, postTo } from '../support/ui';

/*
 * Creates an inactive E2E- artista in the E2E user's agencia and re-saves it without changes.
 * Editing an existing artista is covered by sesion-csrf.spec.ts.
 *
 * The server capitalises the name word by word ("E2E-artista-x" is stored as "E2e-artista-x"),
 * so the prefix is compared ignoring case. Creating an artista also sends an internal message
 * to every active user. The UI cannot delete artistas: sql/1.7.1/01_e2e_cleanup_formularios.sql
 * removes the artista, its accesses and those messages.
 */
const FORM = '#artista-detail-edit-form';

test.describe.serial('Guardar artista', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);

  let idArtista = '';
  let nombre = '';

  test('crea un artista E2E- inactivo', async ({ page }) => {
    const idUsuario = await miUsuarioId(page);
    await page.goto('/artista/mis-artistas');
    const nuevo = page.getByRole('link', { name: 'Nuevo artista' });
    test.skip((await nuevo.count()) === 0, 'El usuario E2E no tiene permiso ARTISTA_CREAR en ninguna agencia.');
    await nuevo.click();

    nombre = e2eId('artista');
    const form = page.locator(FORM);
    await form.locator('#nombre').fill(nombre);
    await chooseOptionByValue(page, 'usuario', idUsuario);
    await chooseFirstOption(page, 'tiposArtista');
    await chooseFirstOption(page, 'comunidadesTrabajo');
    // Keep the test artista out of listados, public pages and OrquestasDeGalicia.
    for (const name of ['activo', 'tarifasPublicas', 'publicarEventos', 'permitirSuscripcionCalendario', 'permiteOrquestasDeGalicia']) {
      const checkbox = form.locator(`input[type="checkbox"][name="${name}"]`);
      if ((await checkbox.count()) > 0 && (await checkbox.isChecked())) {
        await checkbox.uncheck();
      }
    }

    const responsePromise = postTo(page, '/artista/guardar');
    await form.getByRole('button', { name: 'Crear artista' }).click();
    expect((await responsePromise).status()).toBe(302);

    await expect(page).toHaveURL(/\/artista\/\d+$/);
    await expect(alertWith(page, 'Artista guardado correctamente')).toBeVisible();
    idArtista = page.url().match(/\/artista\/(\d+)$/)?.[1] ?? '';

    await page.goto(`/artista/edit/${idArtista}`);
    expect((await page.locator(`${FORM} #nombre`).inputValue()).toLowerCase()).toBe(nombre.toLowerCase());
    await expect(page.locator(`${FORM} input[type="checkbox"][name="activo"]`)).not.toBeChecked();
  });

  test('re-guarda el artista E2E- sin cambios', async ({ page }) => {
    test.skip(idArtista === '', 'No se creó el artista E2E- en la prueba anterior.');

    await page.goto(`/artista/edit/${idArtista}`);
    const form = page.locator(FORM);
    const before = await formSnapshot(form);

    const responsePromise = postTo(page, '/artista/guardar');
    await form.getByRole('button', { name: 'Guardar', exact: true }).click();
    expect((await responsePromise).status()).toBe(302);

    await expect(page).toHaveURL(new RegExp(`/artista/${idArtista}$`));
    await expect(alertWith(page, 'Artista guardado correctamente')).toBeVisible();

    await page.goto(`/artista/edit/${idArtista}`);
    expect(await formSnapshot(page.locator(FORM))).toEqual(before);
  });
});
