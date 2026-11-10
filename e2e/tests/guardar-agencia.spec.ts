import { expect, test } from '@playwright/test';
import { E2E_ARTISTA_ID, MISSING_ARTISTA, MISSING_CREDENTIALS, e2eId, hasCredentials } from '../support/env';
import { agenciaDelArtista, alertWith, chooseOptionByValue, formSnapshot, miUsuarioId, postTo } from '../support/ui';

/*
 * Saving an agencia sets its administrador's general role to "agencia" and, when that user is not
 * the one saving, invalidates their session. Both tests only run when the E2E user is the
 * administrador of their own agencia, so the save never touches another user.
 * The UI cannot delete agencias: a created one is removed by sql/1.7.1/01_e2e_cleanup_formularios.sql.
 */
const FORM = 'form[action="/agencia/guardar"]';

test.describe('Guardar agencia', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(E2E_ARTISTA_ID === '', MISSING_ARTISTA);

  let idUsuario = '';
  let idAgencia = '';
  let esAdministrador = false;

  test.beforeEach(async ({ page }) => {
    idUsuario = await miUsuarioId(page);
    idAgencia = await agenciaDelArtista(page, E2E_ARTISTA_ID);
    await page.goto(`/agencia/${idAgencia}`);
    const editar = page.getByRole('link', { name: 'Editar agencia' });
    test.skip(
      (await editar.count()) === 0,
      `El usuario E2E no tiene permiso AGENCIA_EDITAR sobre la agencia ${idAgencia}.`,
    );
    await page.goto(`/agencia/edit/${idAgencia}`);
    esAdministrador = (await page.locator(`${FORM} #usuario`).inputValue()) === idUsuario;
  });

  test('re-guarda la agencia del usuario E2E sin cambios', async ({ page }) => {
    test.skip(!esAdministrador, `El usuario E2E no es el administrador de la agencia ${idAgencia}; guardarla cerraría la sesión de otro usuario.`);

    const form = page.locator(FORM);
    const before = await formSnapshot(form);

    const responsePromise = postTo(page, '/agencia/guardar');
    await form.getByRole('button', { name: 'Guardar', exact: true }).click();
    expect((await responsePromise).status()).toBe(302);

    await expect(page).toHaveURL(new RegExp(`/agencia/${idAgencia}$`));
    await expect(alertWith(page, 'Agencia guardada correctamente')).toBeVisible();

    await page.goto(`/agencia/edit/${idAgencia}`);
    expect(await formSnapshot(page.locator(FORM))).toEqual(before);
  });

  test('crea una agencia E2E- inactiva administrada por el usuario E2E', async ({ page }) => {
    await page.goto('/agencia');
    test.skip(
      (await page.locator('a[href="/agencia/crear"]').count()) === 0,
      'El usuario E2E no tiene permiso AGENCIA_CREAR: no se prueba la creación de agencias.',
    );
    test.skip(!esAdministrador, 'El usuario E2E no administra ninguna agencia; crear una cambiaría su rol general.');

    const nombre = e2eId('agencia');
    await page.locator('a[href="/agencia/crear"]').click();
    const form = page.locator(FORM);
    await form.locator('#nombre').fill(nombre);
    await chooseOptionByValue(page, 'usuario', idUsuario);
    // Keep the test agencia out of listados and public pages.
    await form.locator('input[name="tarifasPublicas"]').uncheck();
    await form.locator('input[name="activo"]').uncheck();

    const responsePromise = postTo(page, '/agencia/guardar');
    await form.getByRole('button', { name: 'Crear agencia' }).click();
    expect((await responsePromise).status()).toBe(302);

    await expect(page).toHaveURL(/\/agencia\/\d+$/);
    await expect(alertWith(page, 'Agencia guardada correctamente')).toBeVisible();
    await expect(page.getByText(nombre)).toBeVisible();
  });
});
