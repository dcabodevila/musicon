# e2e-guardados-formularios

Playwright e2e tests for every save of every authenticated form, run against the local dev app.
Branch: `fix/403-view-in-jar` (tests validate the multipart part-limit fix). Scope and user chosen by the user (L2, L3).

## Specs

- S1 "escribe tests e2e de todos los guardados de todos los formularios y pruebalo"
- S2 Scope "Seguros + creaciones E2E": "Ediciones re-guardando sin cambios (artista, agencia, usuario/perfil, ajustes, accesos, incrementos, opciones de listado), creaciones con prefijo E2E- y su borrado si la UI lo permite (ajuste, documento, artista/agencia nuevos), y ampliar el script SQL de limpieza. Excluye correos, ODG, token de calendario, registro y passkeys."
- S3 User: "El usuario E2E de e2e/.env": "Los tests se saltan (skip con mensaje) lo que ese usuario no pueda hacer. No guardo credenciales nuevas."
- S4 Each save test asserts the user-visible success signal (redirect + flash/alert or toast text) and that no 403/405/500 happens; re-saves keep the data unchanged.
- S5 New rows use the `E2E-` prefix; rows the UI cannot delete are covered by a manual cleanup script under `sql/1.7.1/`.

## Tasks

- T1 S1 inline: commit the multipart part-limit fix, 403 view and access-denied logging. Commit: c496f4a
- T2 S1-S4 worker: artista edit re-save and create (E2E-). Commit: pending
- T3 S1-S4 worker: agencia edit re-save and create (E2E-). Commit: pending
- T4 S1-S4 worker: usuario mi-perfil / editar re-save. Commit: pending
- T5 S1-S5 worker: ajustes create/edit/delete and opciones de listado. Commit: pending
- T6 S1-S4 worker: accesos de agencia y de artista. Commit: pending
- T7 S1-S5 worker: incrementos y documentos. Commit: pending
- T8 S5 worker: cleanup SQL for new E2E- rows. Commit: pending
- T9 S1 inline: run the whole e2e suite and report. Commit: n/a

## Log

- L1 User: "escribe tests e2e de todos los guardados de todos los formularios y pruebalo"
- L2 User chose scope: "Seguros + creaciones E2E (Recomendado)".
- L3 User chose: "El usuario E2E de e2e/.env".
- L4 Inventory (explorer): saves already covered: ocupacion save/anular (ocupaciones.spec.ts), tarifa save/eliminar (tarifas.spec.ts), listado generar (listados.spec.ts), artista edit (sesion-csrf.spec.ts). Local app runs with dummy Cloudinary credentials, so file uploads may fail locally.
