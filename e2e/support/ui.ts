import { expect, type Locator, type Page } from '@playwright/test';

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

/** Values currently selected in a (possibly Choices-enhanced) native select. */
export async function selectedValues(page: Page, selectId: string): Promise<string[]> {
  return page
    .locator(`#${selectId}`)
    .evaluate((el) => Array.from((el as HTMLSelectElement).selectedOptions).map((o) => o.value).filter((v) => v !== ''));
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
