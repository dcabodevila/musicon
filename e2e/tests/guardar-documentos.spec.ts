import { expect, test } from '@playwright/test';
import { E2E_ARTISTA_ID, MISSING_ARTISTA, MISSING_CREDENTIALS, e2eId, hasCredentials } from '../support/env';
import { alertWith, postTo } from '../support/ui';

/*
 * Uploads an E2E- PDF to the artista documents (/documentos/{id}) and deletes it from the same
 * page. Files are stored in Cloudinary: with the dummy credentials of a local run the upload
 * fails and the test is marked as fixme. Deleting only deactivates the row, so
 * sql/1.7.1/01_e2e_cleanup_formularios.sql removes it; the Cloudinary file is not removed.
 */
const PDF = Buffer.from(
  '%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n2 0 obj<</Type/Pages/Kids[]/Count 0>>endobj\n' +
    'trailer<</Root 1 0 R>>\n%%EOF\n',
  'latin1',
);

test.describe('Guardar documentos', () => {
  test.skip(!hasCredentials, MISSING_CREDENTIALS);
  test.skip(E2E_ARTISTA_ID === '', MISSING_ARTISTA);

  test('sube un documento E2E- y lo elimina', async ({ page }) => {
    await page.goto(`/documentos/${E2E_ARTISTA_ID}`);
    const form = page.locator('form[action="/documentos/guardar"]');
    test.skip((await form.count()) === 0, `El usuario E2E no tiene permiso DOCUMENTACION sobre el artista ${E2E_ARTISTA_ID}.`);

    const nombre = `${e2eId('documento')}.pdf`;
    await form.locator('input[name="documento"]').setInputFiles({ name: nombre, mimeType: 'application/pdf', buffer: PDF });

    const responsePromise = postTo(page, '/documentos/guardar');
    await form.getByRole('button', { name: 'Subir archivo' }).click();
    expect((await responsePromise).status()).toBe(302);
    await expect(page).toHaveURL(new RegExp(`/documentos/${E2E_ARTISTA_ID}$`));

    const error = page.locator('[role="alert"].alert-danger');
    if ((await error.count()) > 0) {
      test.fixme(
        true,
        `La subida necesita un almacenamiento Cloudinary real; con las credenciales de prueba el servidor responde: "${(await error.innerText()).trim()}".`,
      );
    }
    await expect(alertWith(page, 'Documento subido correctamente')).toBeVisible();
    const fila = page.locator('table tbody tr').filter({ hasText: nombre });
    await expect(fila).toHaveCount(1);

    await fila.locator('a[href^="/documentos/eliminar/"]').click();
    await expect(page).toHaveURL(new RegExp(`/documentos/${E2E_ARTISTA_ID}$`));
    await expect(alertWith(page, 'Documento eliminado')).toBeVisible();
    await expect(page.locator('table tbody tr').filter({ hasText: nombre })).toHaveCount(0);
  });
});
