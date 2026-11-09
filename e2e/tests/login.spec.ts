import { expect, test } from '@playwright/test';
import { E2E_PASSWORD, E2E_USER, MISSING_CREDENTIALS, hasCredentials } from '../support/env';

// Fresh browser context: no stored session.
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Login', () => {
  test('una página protegida sin sesión redirige al login', async ({ page }) => {
    const response = await page.goto('/listado');

    await expect(page).toHaveURL(/\/auth\/login$/);
    expect(response?.status()).toBe(200);
    await expect(page.locator('form[action="/auth/login"]')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Iniciar sesión' })).toBeVisible();
  });

  test.describe('con el usuario E2E', () => {
    test.skip(!hasCredentials, MISSING_CREDENTIALS);

    test('una contraseña incorrecta vuelve al login con error y sin sesión', async ({ page }) => {
      await page.goto('/auth/login');
      await page.locator('#username').fill(E2E_USER);
      await page.locator('#password').fill(`${E2E_PASSWORD}-incorrecta`);
      await page.getByRole('button', { name: 'Iniciar sesión' }).click();

      await expect(page).toHaveURL(/\/auth\/login\?error$/);
      await expect(page.getByRole('alert')).toHaveText('Usuario o contraseña no válidos.');

      await page.goto('/usuarios/mi-perfil');
      await expect(page).toHaveURL(/\/auth\/login$/);
    });

    test('las credenciales correctas abren sesión y dan acceso a páginas protegidas', async ({ page }) => {
      await page.goto('/auth/login');
      await page.locator('#username').fill(E2E_USER);
      await page.locator('#password').fill(E2E_PASSWORD);
      await page.getByRole('button', { name: 'Iniciar sesión' }).click();

      await expect(page).not.toHaveURL(/\/auth\/login/);
      await page.goto('/usuarios/mi-perfil');
      await expect(page).toHaveURL(/\/usuarios\/mi-perfil$/);
      await expect(page.getByRole('button', { name: /Cerrar sesión/ }).first()).toBeAttached();
    });

    test('cerrar sesión vuelve al login y la sesión deja de ser válida', async ({ page }) => {
      await page.goto('/auth/login');
      await page.locator('#username').fill(E2E_USER);
      await page.locator('#password').fill(E2E_PASSWORD);
      await page.getByRole('button', { name: 'Iniciar sesión' }).click();
      await expect(page).not.toHaveURL(/\/auth\/login/);

      await page.goto('/usuarios/mi-perfil');
      await page.getByRole('button', { name: /Cerrar sesión/ }).filter({ visible: true }).first().click();

      await expect(page).toHaveURL(/\/auth\/login\?logout$/);
      await page.goto('/usuarios/mi-perfil');
      await expect(page).toHaveURL(/\/auth\/login$/);
    });
  });
});
