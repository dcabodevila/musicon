---
name: feature-pipeline
description: Orquesta el pipeline completo de desarrollo de una feature: análisis de negocio (bosco) → especificación (susan) → implementación (hackerman) → validación (travis, con bucle de corrección) → testing browser (seymour)
---

# Pipeline de Feature Completo

Ejecuta el pipeline completo para desarrollar una nueva funcionalidad en Musicon/GestMusica.

## Argumento de entrada

$ARGUMENTS — descripción de la idea o feature a desarrollar.

---

## Instrucciones de orquestación

Sigue estas fases en orden estricto. Informa al usuario del inicio y fin de cada fase antes de continuar con la siguiente. No saltes fases ni las combines.

---

### FASE 1 — Análisis de negocio (@bosco)

Delega a **bosco** (product-business-analyst) con la idea del usuario.

Pídele que produzca:
- Diagnóstico de valor de negocio
- Riesgos e impacto
- Recomendación clara: ¿vale la pena implementar? ¿con qué alcance?
- Si bosco necesita más información del usuario, interrumpe el pipeline y pregunta. No continúes hasta tener una recomendación clara.

Guarda el output de esta fase como **[OUTPUT_BOSCO]**.

---

### FASE 2 — Especificación técnica (@susan)

Delega a **susan** (spec-analyst) pasándole **[OUTPUT_BOSCO]** como contexto.

Instrúyela a producir la especificación completa siguiendo su formato estándar (secciones 1-11), incluyendo obligatoriamente los casos de prueba funcionales en formato CP-N.

Si susan necesita más contexto o hace preguntas al usuario, interrumpe el pipeline y recoge las respuestas antes de continuar.

Guarda el output como **[OUTPUT_SUSAN]**.

---

### FASE 3 — Implementación (@hackerman)

Delega a **hackerman** (spring-boot-implementer) pasándole **[OUTPUT_SUSAN]** como especificación a implementar.

Instrúyele que:
- Siga estrictamente la especificación de susan
- Implemente todas las capas requeridas (entidad, DTO, repositorio, servicio, controlador, Thymeleaf si aplica)
- Incluya las migraciones SQL necesarias en `/sql/`
- No tome decisiones arquitectónicas que contradigan la spec sin avisar

Guarda el output (código implementado, archivos modificados) como **[OUTPUT_HACKERMAN]**.

---

### FASE 4 — Validación (@travis) con bucle de corrección

Delega a **travis** (dev-code-validator) pasándole **[OUTPUT_SUSAN]** (especificación) y **[OUTPUT_HACKERMAN]** (implementación).

Pídele que valide:
- Correctitud funcional respecto a la spec
- Calidad técnica y adherencia a la arquitectura del proyecto
- Seguridad (OWASP top 10, Spring Security)
- Cobertura de los casos de prueba definidos por susan

**Bucle de corrección (máximo 3 iteraciones):**

Si travis encuentra problemas:
1. Informa al usuario de los problemas encontrados
2. Vuelve a **hackerman** con el feedback de travis como contexto
3. Hackerman corrige
4. Vuelve a **travis** para re-validar
5. Repite hasta aprobación o hasta 3 iteraciones

Si tras 3 iteraciones travis sigue encontrando problemas bloqueantes, detén el pipeline e informa al usuario para decisión manual.

Guarda el resultado de aprobación como **[OUTPUT_TRAVIS]**.

---

### FASE 5 — Testing browser (@seymour)

Delega a **seymour** (qa-browser-tester) pasándole:
- **[OUTPUT_SUSAN]** — casos de prueba funcionales (CP-N) como plan de pruebas
- **[OUTPUT_TRAVIS]** — validación técnica como referencia
- Contexto de qué se ha implementado y en qué módulos

Pídele que ejecute los casos de prueba en el navegador (puerto 8081 por defecto) y genere un informe de resultados estructurado con estado de cada CP-N (PASS / FAIL / BLOCKED).

---

## Informe final

Al completar todas las fases, presenta al usuario un resumen ejecutivo:

```
## Pipeline completado: [nombre de la feature]

| Fase | Agente | Estado | Observaciones |
|------|--------|--------|---------------|
| Análisis de negocio | bosco | ✅ | ... |
| Especificación | susan | ✅ | N casos de prueba definidos |
| Implementación | hackerman | ✅ | N archivos modificados |
| Validación | travis | ✅ | N iteraciones necesarias |
| Testing | seymour | ✅/⚠️ | N/M casos pasados |

### Resultado: LISTO PARA MERGE / PENDIENTE DE CORRECCIONES
```

---

## Reglas generales

- **Nunca continúes** a la siguiente fase si la actual tiene bloqueantes sin resolver
- **Siempre informa** al usuario antes de cambiar de fase
- **Preserva el contexto** entre fases — cada agente necesita el output de los anteriores
- **Interrumpe el pipeline** si el usuario necesita tomar una decisión que los agentes no pueden tomar solos
