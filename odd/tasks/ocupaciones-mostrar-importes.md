# Feature: Ocupaciones "Mostrar Importes" filter checkbox

Goal: add a "Mostrar Importes" checkbox to the Ocupaciones list filter. When checked, the list shows the
importe column and the PDF export includes the importe of each ocupacion.

Decisions:
- No permission gating (user decision): any user who can see the list can see importes when checked.
- Excel export is out of scope (`descargarOcupacionesExcel()` is referenced in markup but not implemented).

## Tasks
- [ ] 1. Backend: `mostrarImportes` flag in `OcupacionListFilterDto`, `/ocupacion/list/data` returns importe when set, PDF export prints importe column when set (jrxml + DTO + param). Focused unit test.
- [ ] 2. Frontend: checkbox in filter form (`ocupaciones.html`), sent with DataTables AJAX, importe column toggled in `ocupaciones-list.js`, PDF export sends flag.

## Evidence
