import { defineConfig, devices } from '@playwright/test';
import { BASE_URL, HEADLESS, STORAGE_STATE } from './support/env';

/**
 * E2E suite against the dev app that is already running (local database).
 *
 * The app allows one session per user (maximumSessions(1)), so the order matters:
 *  1. `setup` logs in once and stores the session in e2e/.auth/user.json.
 *  2. `autenticado` reuses that session for listados, ocupaciones and tarifas.
 *  3. `anonimo` runs last, because its fresh logins would expire the stored session. It is the
 *     `teardown` of `setup`, not a dependent of `autenticado`: a dependent project is skipped
 *     when its dependency fails, while a teardown runs after every project that depends on
 *     `setup` has finished, whatever its result.
 * One worker keeps the data flows (create -> edit -> delete) deterministic.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  forbidOnly: !!process.env.CI,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    headless: HEADLESS,
    locale: 'es-ES',
    timezoneId: 'Europe/Madrid',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off',
  },
  projects: [
    {
      name: 'setup',
      testMatch: /auth\.setup\.ts/,
      teardown: 'anonimo',
      use: { ...devices['Desktop Chrome'], headless: HEADLESS },
    },
    {
      name: 'autenticado',
      testMatch: /(listados|ocupaciones|tarifas|sincronizacion)\.spec\.ts/,
      dependencies: ['setup'],
      use: { ...devices['Desktop Chrome'], headless: HEADLESS, storageState: STORAGE_STATE },
    },
    {
      name: 'anonimo',
      testMatch: /(login|registro)\.spec\.ts/,
      use: { ...devices['Desktop Chrome'], headless: HEADLESS },
    },
  ],
});
