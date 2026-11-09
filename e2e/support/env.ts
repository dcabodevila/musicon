import fs from 'node:fs';
import path from 'node:path';

// Load e2e/.env (ignored by git) without extra dependencies. Variables already set in the
// shell win over the file, so CI or a one-off run can override any value.
const envFile = path.resolve(__dirname, '..', '.env');
if (fs.existsSync(envFile)) {
  process.loadEnvFile(envFile);
}

export const BASE_URL = (process.env.E2E_BASE_URL || 'http://localhost:8081').replace(/\/+$/, '');
export const E2E_USER = process.env.E2E_USER || '';
export const E2E_PASSWORD = process.env.E2E_PASSWORD || '';
export const E2E_ARTISTA_ID = process.env.E2E_ARTISTA_ID || '';
export const HEADLESS = process.env.E2E_HEADLESS === '1';

export const STORAGE_STATE = path.resolve(__dirname, '..', '.auth', 'user.json');

export const hasCredentials = E2E_USER !== '' && E2E_PASSWORD !== '';
export const MISSING_CREDENTIALS =
  'E2E_USER/E2E_PASSWORD no definidos: copia e2e/.env.example a e2e/.env y rellena las credenciales del usuario E2E.';
export const MISSING_ARTISTA =
  'E2E_ARTISTA_ID no definido: indica en e2e/.env el id de un artista sobre el que el usuario E2E tenga permisos.';

/** Prefix shared by every row the suite creates, so leftovers are easy to find and clean. */
export const E2E_PREFIX = 'E2E-';

export function e2eId(label: string): string {
  return `${E2E_PREFIX}${label}-${Date.now().toString(36)}`;
}
