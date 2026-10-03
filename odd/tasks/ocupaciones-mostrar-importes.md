# Feature: Ocupaciones "Mostrar Importes" filter checkbox

Goal: add a "Mostrar Importes" checkbox to the Ocupaciones list filter. When checked, the list shows the
importe column and the PDF export includes the importe of each ocupacion.

Decisions:
- No permission gating (user decision): any user who can see the list can see importes when checked.
- Excel export is out of scope (`descargarOcupacionesExcel()` is referenced in markup but not implemented).

## Tasks
- [x] 1. Backend: `mostrarImportes` flag in `OcupacionListFilterDto`, `/ocupacion/list/data` returns importe when set, PDF export prints importe column when set (jrxml + DTO + param). Focused unit test.
- [x] 2. Frontend: checkbox in filter form (`ocupaciones.html`), sent with DataTables AJAX, importe column toggled in `ocupaciones-list.js`, PDF export sends flag.
- [x] 3. PDF: no blank Importe gap when mostrarImportes is off (Representante keeps full width).
- [x] 4. List: Importe column right-aligned.
- [x] 5. Filter: Agencia and Artista selects on the same row.
- [x] 6. Security: list screen endpoints (/ocupacion/list GET/POST, /list/data, /ocupaciones-pdf, /ocupaciones-excel, /artista/artistas/{idAgencia}) require access to the requested agencia (agencia in user's mapPermisosAgencia; admins map all agencias); 403 otherwise. No idAgencia keeps only the existing artist OCUPACIONES restriction (user decision: users with artist-only access keep visibility).
- [ ] 7. Importes gated by `VER_DATOS_ECONOMICOS` per artist: checkbox shown only if the user has it on at least one artist; list and PDF fill importe only for rows whose artist grants it (blank otherwise), even if `mostrarImportes=true` is forced. Supersedes the earlier "no permission gating" decision (artist role lost `VER_DATOS_ECONOMICOS` in sql/1.0.7, so importes leaked).

## Evidence
- Task 1: commit e662e44. Tests OcupacionMostrarImportesTest (2) + OcupacionListadoJrxmlTest (1) green on JDK 17. Representante PDF column narrowed 160->110 to fit Importe (50).
- Task 2: commit 423d8b6. node --check + mvnw compile (JDK 17) OK. No automated UI test; manual browser check pending.
- Rebased onto develop (= main 6190821); resolved toDataTableRow conflict keeping lat/lng + importe. Follow-up commit 52411f4. 77 ocupacion tests green (JDK 17).
- Task 3: commit 939fd26. Report fill tests: Representante width 160 without importes, 110 with; RED observed (110 != 160).
- Task 4: commit a6bba48 (className text-end). Task 5: commit 4062380 (template row). Structural checks only.
- Task 6: commit a1ea3a7. AccessDeniedException -> 403 via accessDeniedPage. 10 focused tests green; RED observed reverting OcupacionController (4 failures).
- Follow-up: commit 33e072d, filter labels above inputs (row g-3 / col-md-3 / form-label, as in ocupaciones-form). Structural check only.
