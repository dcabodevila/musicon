# AGENTS.md — musicon

## Ámbito
Estas instrucciones aplican a todo el repositorio `musicon`.

## Reglas obligatorias
- Responde y escribe documentación en **es-ES**.
- Usa **Git convencional** para cualquier commit: sin coautorías ni atribuciones de IA.
- **No hagas build** tras cambios. Si hace falta verificar, usa solo pruebas concretas.
- **No modifiques la BBDD local** directamente: genera scripts SQL en `/sql/x.y.z` para ejecución manual.
- Mantén la arquitectura existente: monolito Spring Boot por feature, con capas `Controller` / `Service` / `Repository` / `Entity`.

## Páginas públicas SEO
- Las rutas bajo `/eventos/**`, `/baja/**` y `robots.txt` deben seguir **stateless** y sin CSRF.
- En plantillas públicas usa `header-css-public`, nunca `header-css`.
- Conserva `title`, `description`, `robots`, `canonical`, OG y JSON-LD en páginas públicas.
- En listados, filtros, paginación y vacíos, usa `noindex,follow` cuando corresponda.

## Calidad y verificación
- Prioriza pruebas automáticas relevantes antes que cambios amplios.
- Si el cambio afecta SEO, valida también HTML renderizado, canonical y structured data.
- Si tocas tests, manténlos enfocados y representativos del contrato público.

## Memoria y trazabilidad
- Guarda decisiones relevantes, bugs y hallazgos importantes en **Engram**.
- Al cerrar una sesión importante, deja un resumen persistente.

## Estilo de trabajo
- Haz cambios pequeños y acotados.
- Evita tocar artefactos no relacionados.
- Si hay dudas de alcance, verifica primero antes de asumir.
