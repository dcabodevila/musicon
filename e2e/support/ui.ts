import { expect, type Locator, type Page, type Response } from '@playwright/test';

/** Notyf toast shown by `notif(type, message)` in static/js/comun.js. */
export function toast(page: Page, type: 'success' | 'error' | 'warning'): Locator {
  return page.locator(`.notyf__toast--${type} .notyf__message`);
}

/** Any success-like toast (`notif` uses the server `messageType`, which may be `warning`). */
export function okToast(page: Page): Locator {
  return page.locator('.notyf__toast--success .notyf__message, .notyf__toast--warning .notyf__message');
}

/**
 * Sets a flatpickr date the same way a click in its calendar does: through the instance the
 * page JS attached to the input, firing its onChange handlers. The visible field is the
 * flatpickr alt input and the real one is read-only, so typing is not possible.
 */
export async function setFlatpickrDate(page: Page, inputId: string, ddmmyyyy: string): Promise<void> {
  await page.locator(`#${inputId}`).evaluate((el, value) => {
    const fp = (el as HTMLInputElement & { _flatpickr?: { setDate(d: string, fire: boolean, fmt: string): void } })
      ._flatpickr;
    if (!fp) {
      throw new Error(`flatpickr is not initialised on #${el.id}`);
    }
    fp.setDate(value, true, 'd-m-Y');
  }, ddmmyyyy);
  await expect(page.locator(`#${inputId}`)).toHaveValue(ddmmyyyy);
}

/**
 * Picks the first real option of a Choices.js select by clicking its dropdown, like a user.
 * Choices hides the native <select> and keeps only the selected options in it.
 */
export async function chooseFirstOption(page: Page, selectId: string): Promise<void> {
  const wrapper = page.locator('.choices').filter({ has: page.locator(`#${selectId}`) });
  await wrapper.click();
  await wrapper
    .locator('.choices__list--dropdown .choices__item--choice.choices__item--selectable:not(.choices__placeholder)')
    .first()
    .click();
  await page.keyboard.press('Escape');
}

/** Picks the option with this value in a Choices.js select by clicking its dropdown item. */
export async function chooseOptionByValue(page: Page, selectId: string, value: string): Promise<void> {
  const wrapper = page.locator('.choices').filter({ has: page.locator(`#${selectId}`) });
  await wrapper.click();
  await wrapper.locator(`.choices__list--dropdown .choices__item--choice[data-value="${value}"]`).click();
  await page.keyboard.press('Escape');
}

/** Value of the option with this text in a Choices.js select (its native select only keeps the selected ones). */
export async function choicesOptionValue(page: Page, selectId: string, text: string): Promise<string | null> {
  const wrapper = page.locator('.choices').filter({ has: page.locator(`#${selectId}`) });
  const item = wrapper.locator('.choices__list--dropdown .choices__item--choice').filter({ hasText: text });
  return (await item.count()) === 0 ? null : item.first().getAttribute('data-value');
}

/** Values currently selected in a (possibly Choices-enhanced) native select. */
export async function selectedValues(page: Page, selectId: string): Promise<string[]> {
  return page
    .locator(`#${selectId}`)
    .evaluate((el) => Array.from((el as HTMLSelectElement).selectedOptions).map((o) => o.value).filter((v) => v !== ''));
}

/** Waits for the POST to `path` (exact pathname) triggered by the next action. */
export function postTo(page: Page, path: string): Promise<Response> {
  return page.waitForResponse((r) => r.request().method() === 'POST' && new URL(r.url()).pathname === path);
}

/** Flash message rendered by the templates as `<div role="alert">` after a redirect. */
export function alertWith(page: Page, text: string): Locator {
  return page.getByRole('alert').filter({ hasText: text });
}

/**
 * Text fields a form would submit right now, without the CSRF token or files. Comparing two
 * snapshots proves that a re-save left every visible value as it was.
 */
export async function formSnapshot(form: Locator): Promise<string[][]> {
  return form.evaluate((el) =>
    [...new FormData(el as HTMLFormElement).entries()]
      .filter(([name, value]) => name !== '_csrf' && typeof value === 'string')
      .map(([name, value]) => [name, value as string]),
  );
}

/** Id of the logged-in user, read from the hidden id field of "Mi perfil". */
export async function miUsuarioId(page: Page): Promise<string> {
  await page.goto('/usuarios/mi-perfil');
  const id = await page.locator('form[action="/usuarios/guardar"] input[name="id"]').inputValue();
  expect(id).not.toBe('');
  return id;
}

/** Agencia of an artista, read from the agencia link of its detail page. */
export async function agenciaDelArtista(page: Page, idArtista: string): Promise<string> {
  await page.goto(`/artista/${idArtista}`);
  const href = await page.locator('main a[href^="/agencia/"]').first().getAttribute('href');
  const id = href?.match(/^\/agencia\/(\d+)$/)?.[1];
  expect(id, `el artista ${idArtista} no muestra enlace a su agencia`).toBeTruthy();
  return id as string;
}

/** dd-MM-yyyy as expected by the app pickers and by the /tarifa/{id}/{fecha} endpoint. */
export function formatDdMmYyyy(date: Date): string {
  const dd = String(date.getUTCDate()).padStart(2, '0');
  const mm = String(date.getUTCMonth() + 1).padStart(2, '0');
  return `${dd}-${mm}-${date.getUTCFullYear()}`;
}

/** A random day in a year reserved for e2e data, far from any real booking or tarifa. */
export function randomDayIn(year: number, maxOffsetDays = 360): Date {
  const offset = Math.floor(Math.random() * maxOffsetDays);
  return new Date(Date.UTC(year, 0, 1) + offset * 86_400_000);
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getTime() + days * 86_400_000);
}
