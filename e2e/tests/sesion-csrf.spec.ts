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
});
