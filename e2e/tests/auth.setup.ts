import fs from 'node:fs';
import path from 'node:path';
import { expect, test as setup } from '@playwright/test';
import { E2E_PASSWORD, E2E_USER, MISSING_CREDENTIALS, STORAGE_STATE, hasCredentials } from '../support/env';

setup('inicia sesión con el usuario E2E y guarda la sesión', async ({ page }) => {
  setup.skip(!hasCredentials, MISSING_CREDENTIALS);

  await page.goto('/auth/login');
  await page.locator('#username').fill(E2E_USER);
  await page.locator('#password').fill(E2E_PASSWORD);
  await page.getByRole('button', { name: 'Iniciar sesión' }).click();

  await expect(page).not.toHaveURL(/\/auth\/login/);
  await page.goto('/usuarios/mi-perfil');
  await expect(page).toHaveURL(/\/usuarios\/mi-perfil$/);

  fs.mkdirSync(path.dirname(STORAGE_STATE), { recursive: true });
  await page.context().storageState({ path: STORAGE_STATE });
});
