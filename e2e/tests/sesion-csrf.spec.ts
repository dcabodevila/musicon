import { expect, test } from '@playwright/test';
import {
  E2E_ARTISTA_ID,
  E2E_PASSWORD,
  E2E_USER,
  MISSING_ARTISTA,
  MISSING_CREDENTIALS,
  hasCredentials,
} from '../support/env';

// Own login with "Recordarme": the scenario needs the remember-me cookie, not the stored session.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Sesión perdida con "Recordarme"', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(!E2E_ARTISTA_ID, MISSING_ARTISTA);

  test('un formulario abierto antes de perder la sesión se guarda sin error 405', async ({ page, context }) => {
    await page.goto('/auth/login');
    await page.locator('#username').fill(E2E_USER);
    await page.locator('#password').fill(E2E_PASSWORD);
    await page.locator('#remember-me').check();
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page).not.toHaveURL(/\/auth\/login/);

    await page.goto(`/artista/edit/${E2E_ARTISTA_ID}`);
    const guardar = page.getByRole('button', { name: 'Guardar', exact: true });
    await expect(guardar).toBeVisible();

    const cookieNames = (await context.cookies()).map((cookie) => cookie.name);
    expect(cookieNames).toEqual(expect.arrayContaining(['JSESSIONID', 'remember-me', 'XSRF-TOKEN']));

    // Server session gone (expiry, restart or deploy) while the form stays open.
    await context.clearCookies({ name: 'JSESSIONID' });

    const saveResponse = page.waitForResponse(
      (response) => response.url().endsWith('/artista/guardar') && response.request().method() === 'POST',
    );
    await guardar.click();
    expect((await saveResponse).status()).toBe(302);

    await expect(page).toHaveURL(new RegExp(`/artista/${E2E_ARTISTA_ID}$`));
    await expect(page.getByRole('alert').filter({ hasText: 'Artista guardado correctamente' })).toBeVisible();
  });

  test('un formulario con más de 50 campos se guarda (límite de partes multipart de Tomcat)', async ({ page }) => {
    await page.goto('/auth/login');
    await page.locator('#username').fill(E2E_USER);
    await page.locator('#password').fill(E2E_PASSWORD);
    await page.getByRole('button', { name: 'Iniciar sesión' }).click();
    await expect(page).not.toHaveURL(/\/auth\/login/);

    await page.goto(`/artista/edit/${E2E_ARTISTA_ID}`);
    // An artista working in many comunidades sends one part per checked option. Pad the form with fields the
    // server ignores so it goes past Spring Boot's default of 50 parts without changing the artista's data.
    const parts = await page.locator('#artista-detail-edit-form').evaluate((form: HTMLFormElement) => {
      for (let i = 0; i < 40; i++) {
        const input = document.createElement('input');
        input.type = 'hidden';
        input.name = `e2ePadding${i}`;
        input.value = 'x';
        form.appendChild(input);
      }
      return [...new FormData(form).keys()].length;
    });
    expect(parts).toBeGreaterThan(50);

    const saveResponse = page.waitForResponse(
      (response) => response.url().endsWith('/artista/guardar') && response.request().method() === 'POST',
    );
    await page.getByRole('button', { name: 'Guardar', exact: true }).click();
    expect((await saveResponse).status()).toBe(302);

    await expect(page).toHaveURL(new RegExp(`/artista/${E2E_ARTISTA_ID}$`));
    await expect(page.getByRole('alert').filter({ hasText: 'Artista guardado correctamente' })).toBeVisible();
  });
});
