# Feature: Ocupaciones "Mostrar Importes" filter checkbox

Goal: add a "Mostrar Importes" checkbox to the Ocupaciones list filter. When checked, the list shows the
importe column and the PDF export includes the importe of each ocupacion.

Decisions:
- No permission gating (user decision): any user who can see the list can see importes when checked.
- Excel export is out of scope (`descargarOcupacionesExcel()` is referenced in markup but not implemented).

## Tasks
- [x] 1. Backend: `mostrarImportes` flag in `OcupacionListFilterDto`, `/ocupacion/list/data` returns importe when set, PDF export prints importe column when set (jrxml + DTO + param). Focused unit test.
- [x] 2. Frontend: checkbox in filter form (`ocupaciones.html`), sent with DataTables AJAX, importe column toggled in `ocupaciones-list.js`, PDF export sends flag.

## Evidence
- Task 1: commit e662e44. Tests OcupacionMostrarImportesTest (2) + OcupacionListadoJrxmlTest (1) green on JDK 17. Representante PDF column narrowed 160->110 to fit Importe (50).
- Task 2: commit 423d8b6. node --check + mvnw compile (JDK 17) OK. No automated UI test; manual browser check pending.
