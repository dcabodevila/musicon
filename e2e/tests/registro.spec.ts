import { expect, type Page, test } from '@playwright/test';
import { E2E_USER, e2eId } from '../support/env';
import { chooseFirstOption } from '../support/ui';

/*
 * Only validation paths are covered: every case is rejected before
 * UserService.saveRegistration, so no user is created.
 * A full registration is not automated: it sends a real verification email and the new
 * account cannot log in until the emailed code is entered at /auth/verify-email, and users
 * cannot be deleted through the UI.
 */
test.use({ storageState: { cookies: [], origins: [] } });

async function fillValidForm(page: Page, username: string): Promise<void> {
  const id = username.toLowerCase();
  await page.locator('#username').fill(username);
  await page.locator('#email').fill(`${id}@example.com`);
  await page.locator('#password').fill('E2e-Password-123');
  await page.locator('#retryPassword').fill('E2e-Password-123');
  await page.locator('#nombre').fill(username);
  await page.locator('#apellidos').fill('E2E');
  await page.locator('#nombreComercial').fill(username);
  await page.locator('#telefono').fill('600000000');
  await chooseFirstOption(page, 'provincia');
  await page.locator('#tipoUsuario').selectOption('ARTISTA');
  await page.locator('#aceptaPolitica').check();
}

function fieldErrors(page: Page, fieldId: string) {
  return page.locator(`#${fieldId}`).locator('xpath=ancestor::div[contains(@class,"col-12")][1]').locator('.registration-form__error p');
}

test.describe('Registro', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/auth/registration');
    await expect(page.getByRole('button', { name: 'Crear cuenta profesional' })).toBeVisible();
  });

  test('el navegador no envía el formulario con campos obligatorios vacíos', async ({ page }) => {
    let posted = false;
    page.on('request', (request) => {
      if (request.method() === 'POST' && request.url().endsWith('/auth/registration')) {
        posted = true;
      }
    });

    await page.getByRole('button', { name: 'Crear cuenta profesional' }).click();

    await expect(page).toHaveURL(/\/auth\/registration$/);
    expect(await page.locator('#username').evaluate((el) => (el as HTMLInputElement).validity.valueMissing)).toBe(true);
    expect(await page.locator('#aceptaPolitica').evaluate((el) => (el as HTMLInputElement).validity.valueMissing)).toBe(true);
    expect(posted).toBe(false);
  });

  test('contraseñas distintas: el servidor rechaza el registro y conserva los datos', async ({ page }) => {
    const username = e2eId('reg');
    await fillValidForm(page, username);
    await page.locator('#retryPassword').fill('Otra-Password-456');

    const responsePromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().endsWith('/auth/registration'),
    );
    await page.getByRole('button', { name: 'Crear cuenta profesional' }).click();
    const response = await responsePromise;

    // Re-rendered form (200), not the redirect to /auth/verify-email of a saved registration.
    expect(response.status()).toBe(200);
    await expect(page).toHaveURL(/\/auth\/registration$/);
    await expect(fieldErrors(page, 'password')).toHaveText(['Las contraseñas no coinciden']);
    await expect(fieldErrors(page, 'retryPassword')).toHaveText(['Las contraseñas no coinciden']);
    await expect(page.locator('#username')).toHaveValue(username);
  });

  test('nombre de usuario existente: el servidor rechaza el registro', async ({ page }) => {
    test.skip(E2E_USER === '' || E2E_USER.includes('@'), 'Necesita E2E_USER con un nombre de usuario (no un email) ya registrado.');

    await fillValidForm(page, e2eId('reg'));
    await page.locator('#username').fill(E2E_USER);

    const responsePromise = page.waitForResponse(
      (r) => r.request().method() === 'POST' && r.url().endsWith('/auth/registration'),
    );
    await page.getByRole('button', { name: 'Crear cuenta profesional' }).click();
    const response = await responsePromise;

    expect(response.status()).toBe(200);
    await expect(page).toHaveURL(/\/auth\/registration$/);
    await expect(fieldErrors(page, 'username')).toHaveText(['El nombre de usuario ya existe']);
  });
});
