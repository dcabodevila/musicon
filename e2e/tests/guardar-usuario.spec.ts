import { expect, type Page, test } from '@playwright/test';
import { MISSING_CREDENTIALS, hasCredentials } from '../support/env';
import { alertWith, formSnapshot, miUsuarioId, postTo } from '../support/ui';

/*
 * Re-saves the E2E user's own data without changes, from "Mi perfil" and from the edit page the
 * save redirects to. Password and email are never touched: the form has no password field and
 * the email is sent back as it was loaded.
 */
const FORM = 'form[action="/usuarios/guardar"]';

async function resaveAndCheck(page: Page, idUsuario: string): Promise<void> {
  const form = page.locator(FORM);
  await expect(form).toBeVisible();
  const before = await formSnapshot(form);
  expect(before).toContainEqual(['id', idUsuario]);

  const responsePromise = postTo(page, '/usuarios/guardar');
  await form.getByRole('button', { name: 'Guardar', exact: true }).click();
  expect((await responsePromise).status()).toBe(302);

  await expect(page).toHaveURL(new RegExp(`/usuarios/editar/${idUsuario}$`));
  await expect(alertWith(page, 'Usuario actualizado correctamente')).toBeVisible();
  expect(await formSnapshot(page.locator(FORM))).toEqual(before);
}

test.describe('Guardar usuario', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);

  test('re-guarda "Mi perfil" sin cambios', async ({ page }) => {
    const idUsuario = await miUsuarioId(page);
    await resaveAndCheck(page, idUsuario);
  });

  test('re-guarda el usuario desde su página de edición sin cambios', async ({ page }) => {
    const idUsuario = await miUsuarioId(page);
    await page.goto(`/usuarios/editar/${idUsuario}`);
    await resaveAndCheck(page, idUsuario);
  });
});
