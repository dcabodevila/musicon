import { expect, test } from '@playwright/test';
import { E2E_ARTISTA_ID, MISSING_ARTISTA, MISSING_CREDENTIALS, hasCredentials } from '../support/env';
import { agenciaDelArtista, alertWith, formSnapshot, miUsuarioId, postTo } from '../support/ui';

/*
 * Re-saves, without changes, the E2E user's own accesses on the agencia of E2E_ARTISTA_ID. No
 * access is granted to or removed from other users.
 *
 * Saving an agencia access replaces it: the server deletes the row and its artista permissions
 * and creates them again with the same user, rol and artista, so the row id changes.
 * An artista access is updated in place.
 */
test.describe('Guardar accesos', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(E2E_ARTISTA_ID === '', MISSING_ARTISTA);

  let idUsuario = '';
  let idAgencia = '';

  test.beforeEach(async ({ page }) => {
    idUsuario = await miUsuarioId(page);
    idAgencia = await agenciaDelArtista(page, E2E_ARTISTA_ID);
    await page.goto(`/agencia/${idAgencia}`);
    test.skip(
      (await page.getByRole('link', { name: 'Gestión de accesos' }).count()) === 0,
      `El usuario E2E no tiene permiso GESTION_ACCESOS sobre la agencia ${idAgencia}.`,
    );
    await page.goto(`/accesos/${idAgencia}`);
  });

  test('re-guarda sin cambios el acceso a la agencia del usuario E2E', async ({ page }) => {
    const editar = page.locator(`.btnModalAcceso[data-idusuario="${idUsuario}"]`).first();
    test.skip((await editar.count()) === 0, `El usuario E2E no tiene acceso propio en la agencia ${idAgencia}.`);
    const idRol = await editar.getAttribute('data-idrol');
    const idArtista = (await editar.getAttribute('data-idartista')) ?? '';

    await editar.click();
    const modal = page.locator('#modalAccesos');
    // The permissions list is filled by the modal's "shown" handler, which also resets the artista.
    await expect(modal.locator('#lista-permisos li').first()).toBeVisible();
    const form = modal.locator('form');
    await expect(form.locator('#usuarios-accesos')).toHaveValue(idUsuario);
    await expect(form.locator('#rol-accesos')).toHaveValue(idRol ?? '');
    const before = (await formSnapshot(form)).filter(([name]) => name !== 'id');

    const responsePromise = postTo(page, '/accesos/guardar');
    await form.locator('#btn-guardar-acceso').click();
    expect((await responsePromise).status()).toBe(302);

    await expect(page).toHaveURL(new RegExp(`/accesos/${idAgencia}$`));
    await expect(alertWith(page, 'Acceso guardado correctamente')).toBeVisible();
    // Same user, rol and artista after the save (under a new id).
    const after = page.locator(
      `.btnModalAcceso[data-idusuario="${idUsuario}"][data-idrol="${idRol}"]` +
        (idArtista === '' ? '' : `[data-idartista="${idArtista}"]`),
    );
    await expect(after).toHaveCount(1);
    await after.click();
    await expect(modal.locator('#lista-permisos li').first()).toBeVisible();
    expect((await formSnapshot(modal.locator('form'))).filter(([name]) => name !== 'id')).toEqual(before);
  });

  test('re-guarda sin cambios un acceso de artista del usuario E2E', async ({ page }) => {
    const editar = page.locator(`.btnModalAccesoArtista[data-idusuario="${idUsuario}"]`).first();
    test.skip((await editar.count()) === 0, `El usuario E2E no tiene accesos de artista en la agencia ${idAgencia}.`);
    const idAcceso = await editar.getAttribute('data-idacceso');

    await editar.click();
    const modal = page.locator('#modalAccesosArtista');
    await expect(modal).toBeVisible();
    const form = modal.locator('form');
    await expect(form.locator('#id-acceso-artista')).toHaveValue(idAcceso ?? '');
    await expect(form.locator('#usuarios-accesos-artista')).toHaveValue(idUsuario);
    const before = await formSnapshot(form);

    const responsePromise = postTo(page, '/accesos/guardar-acceso-artista');
    await form.locator('#btn-guardar-acceso-artista').click();
    expect((await responsePromise).status()).toBe(302);

    await expect(page).toHaveURL(new RegExp(`/accesos/${idAgencia}$`));
    await expect(alertWith(page, 'Acceso guardado correctamente')).toBeVisible();
    await page.locator(`.btnModalAccesoArtista[data-idacceso="${idAcceso}"]`).click();
    await expect(modal).toBeVisible();
    expect(await formSnapshot(modal.locator('form'))).toEqual(before);
  });
});
