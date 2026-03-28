# SDD Orchestrator UI/UX Template

Usá este template como prompt base para ejecutar cambios con SDD cuando pueda haber impacto en frontend/UX.

---

## Versión corta (uso diario)

```md
Ejecutar fases SDD: sdd-explore → sdd-propose → sdd-spec → sdd-design → sdd-tasks → sdd-apply → sdd-verify.
No saltar fases; si hay bloqueantes, detener y reportar.

Si el cambio toca pantallas, formularios, navegación, componentes UI, estados de interacción o accesibilidad,
antes de cada fase aplicable ejecutar: skill(name="frontend-design").

Aplicar frontend-design en: sdd-explore, sdd-spec, sdd-design,
sdd-apply (solo si hay código frontend), sdd-verify (solo si valida UI/UX).

Objetivo del cambio: {describir feature}
Contexto: {módulo/alcance/restricciones}
```

---

## Versión completa (recomendada para cambios medianos/grandes)

```md
# SDD Orchestrator Prompt Template (UI/UX-aware)

Objetivo del cambio: {describir feature}

## Reglas de ejecución
1. Ejecutar flujo SDD por fases: `sdd-explore` → `sdd-propose` → `sdd-spec` → `sdd-design` → `sdd-tasks` → `sdd-apply` → `sdd-verify`.
2. Si una fase queda bloqueada, detener y reportar.
3. No saltar fases.
4. Mantener trazabilidad entre artefactos (proposal/spec/design/tasks).

## Activación condicional de skill UX
Si el cambio toca cualquiera de estos puntos:
- pantallas/vistas
- formularios
- navegación
- componentes UI
- feedback de interacción (loading/error/success)
- accesibilidad

ENTONCES cargar `frontend-design` ANTES de ejecutar:
- `sdd-explore`
- `sdd-spec`
- `sdd-design`
- `sdd-apply` (solo si hay código frontend)
- `sdd-verify` (solo si se valida UX/UI)

Implementación explícita de carga:
- Antes de cada fase aplicable, ejecutar `skill(name="frontend-design")`.

Si NO toca UI/UX, no cargar `frontend-design`.

## Criterios mínimos UX obligatorios (cuando aplique)
En `sdd-spec`, incluir criterios verificables:
- Estados UI: loading, empty, error, success
- Accesibilidad: navegación por teclado, foco visible, labels/aria, contraste mínimo WCAG AA en elementos críticos
- Mensajes de error claros y accionables
- Tiempo objetivo para tarea principal (time-to-task)

En `sdd-design`, documentar:
- Jerarquía visual (qué se prioriza y por qué)
- Reglas de espaciado/tipografía/color
- Patrones de interacción consistentes (hover/focus/disabled)
- Decisiones responsive (mobile-first o breakpoints definidos)

En `sdd-verify`, validar:
- Cumplimiento de criterios de spec UX
- Consistencia de interacción entre pantallas
- Check rápido de accesibilidad (teclado/foco/labels/contraste)
- Riesgos UX pendientes + severidad

## Salida esperada por fase
- Resumen corto (qué se hizo, qué falta, riesgos)
- Evidencia de decisiones (referencia a spec/design/tasks)
- Próxima fase sugerida
```

---

## Backend-only guard (para cambios sin UI/UX)

Usá este bloque cuando el cambio sea 100% backend (Spring/DB/API/jobs/security) para evitar cargar skills de frontend por error.

```md
## Backend-Only Guard

Clasificar el cambio ANTES de iniciar fases:

Se considera **backend-only** si afecta únicamente:
- entidades/DTO/repositorios/servicios/controladores backend
- SQL/migraciones/configuración
- seguridad/autenticación/autorización
- integraciones externas/schedulers/cache
- endpoints API sin cambios en vistas/componentes/interacción UI

Si backend-only = true:
- NO ejecutar `skill(name="frontend-design")`
- Ejecutar SDD normal: sdd-explore → sdd-propose → sdd-spec → sdd-design → sdd-tasks → sdd-apply → sdd-verify
- En spec/design/verify enfocarse en: contratos API, validaciones, errores, performance, seguridad, observabilidad

Si backend-only = false:
- aplicar el bloque UI/UX y cargar `frontend-design` en fases aplicables.

Checklist de confirmación (obligatorio):
- ¿Se modifica alguna vista/pantalla/componente? (sí/no)
- ¿Cambia flujo de interacción del usuario? (sí/no)
- ¿Se agregan requisitos de accesibilidad visual/teclado? (sí/no)

Si las 3 respuestas son “no” => backend-only = true.
```
