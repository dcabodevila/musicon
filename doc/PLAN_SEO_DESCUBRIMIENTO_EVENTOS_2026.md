# Plan SEO y Descubrimiento de Eventos (Google + Motores Generativos)

Fecha: 2026-03-15  
Alcance: publicación pública de eventos en Festia para consultas tipo "qué artista toca en qué localidad".

## 1) Decisión de arquitectura URL (dominio, subdominio o ruta actual)

### Recomendación principal
Mantener la publicación en la URL actual bajo el mismo dominio: `https://festia.es/eventos`.

Motivos:
- Aprovecha autoridad de dominio existente.
- Evita migración SEO (riesgo de pérdidas temporales de tráfico/indexación).
- Simplifica Search Console, analítica y mantenimiento.

### Cuándo usar subdominio (`eventos.festia.es`)
Solo si hay necesidad técnica/organizativa clara:
- despliegue independiente,
- equipo/producto separado,
- infraestructura o stack distinto.

Si se usa subdominio:
- tratarlo como propiedad separada en Search Console,
- mapear redirecciones 301 desde URLs antiguas,
- mantener canónicas consistentes durante la migración.

### Cuándo usar dominio nuevo
No recomendado para este caso. Solo tendría sentido por motivos de marca/legal totalmente independientes.

---

## 2) Objetivo de indexación

Indexar con prioridad:
1. Hub principal: `/eventos`
2. Landing por provincia: `/eventos/provincia/{provincia}`
3. Landing por artista: `/eventos/artista/{id-o-slug}`
4. Ficha de evento: `/eventos/evento/{id}-{slug}`

No indexar:
- combinaciones de filtros por query params (`?provincia=...&desde=...`) salvo estrategia específica.

---

## 3) Plan por fases

## Fase 0 (48h): desbloqueo técnico mínimo

Objetivo: que Google pueda rastrear y entender contenido sin fricción.

Acciones:
- Permitir acceso público en seguridad a:
  - `/robots.txt`
  - `/eventos/**`
  - `/eventos/sitemap.xml`
- Verificar que `robots.txt` responde `200` y no redirige a login.
- Confirmar que `sitemap` responde `200` y lista URLs públicas válidas.
- Dar de alta propiedad en Google Search Console (recomendado: tipo **Dominio** de `festia.es`).
- Enviar sitemap: `https://festia.es/eventos/sitemap.xml`.

Entregables:
- Captura de "Sitemap enviado" en Search Console.
- Prueba de inspección de URL en 3 páginas reales (hub, artista, evento).

## Fase 1 (1 semana): calidad SEO On-Page

Objetivo: mejorar relevancia por consulta artista + localidad.

Acciones:
- Añadir un único `h1` por plantilla en listados (`/eventos`, `/eventos/provincia/*`, `/eventos/artista/*`).
- Evitar meta description duplicada en `<head>` (dejar solo una por página).
- Mejorar `title`/`description` con patrón semántico:
  - Evento: `{Artista} en {Municipio} ({Provincia}) - {Fecha}`
  - Artista: `Próximas actuaciones de {Artista}`
  - Provincia: `Conciertos y actuaciones en {Provincia}`
- Mantener canonical estable y absoluta en cada tipo de URL.
- Conservar `noindex,follow` en URLs con filtros por query string.

Entregables:
- Validación HTML renderizado de 10 URLs.
- Informe de duplicidades corregidas.

## Fase 2 (1-2 semanas): enlazado interno y rastreabilidad

Objetivo: aumentar descubrimiento de URLs profundas.

Acciones:
- Incluir bloques de enlaces HTML rastreables:
  - "Eventos por provincia"
  - "Eventos por artista"
  - "Próximos eventos"
- No depender solo de formularios/select para navegación.
- Añadir breadcrumbs en fichas de evento:
  - Eventos > Provincia > Artista > Evento.
- Asegurar que cada ficha enlaza al artista y a la provincia.

Entregables:
- Cobertura de rastreo mejorada en Search Console (páginas descubiertas/indexadas).

## Fase 3 (2 semanas): datos estructurados y rich results

Objetivo: mejorar interpretación semántica por Google y otros motores.

Acciones:
- Mantener JSON-LD `MusicEvent` en fichas.
- Completar campos críticos sin valores vacíos:
  - `location.name` (nunca vacío),
  - `startDate` con zona horaria,
  - `performer`,
  - `organizer`,
  - `eventStatus`.
- Añadir cuando exista:
  - `offers` (precio/entrada),
  - `eventAttendanceMode`,
  - `image` representativa del evento/artista.
- Validar con Rich Results Test y Schema validator.

Entregables:
- 0 errores críticos en validación estructurada sobre muestra de URLs.

## Fase 4 (continuo): autoridad y señales externas

Objetivo: mejorar posicionamiento sostenido.

Acciones:
- Conseguir enlaces externos desde:
  - webs de ayuntamientos,
  - promotores,
  - medios locales,
  - perfiles oficiales de artistas.
- Publicar calendario/agenda mensual enlazando a fichas de evento.
- Añadir estrategia de contenidos locales ("Fiestas de {Municipio} 2026").

Entregables:
- Incremento mensual de dominios de referencia.
- Incremento de impresiones y clics en Search Console.

---

## 4) Plan para motores generativos (ChatGPT, Gemini, etc.)

Objetivo: aumentar probabilidad de que las respuestas mencionen eventos de Festia.

Acciones:
- Garantizar acceso público y rastreable a contenido clave (sin login).
- Estructurar datos de eventos de forma consistente (JSON-LD limpio).
- Mantener páginas estables por entidad (evento/artista/provincia) con URLs permanentes.
- Publicar sitemap actualizado y, opcionalmente, feed JSON público de eventos.
- Evitar contenido ambiguo o placeholders ("Provisional") en páginas indexables.
- Reforzar autoridad de fuente mediante enlaces/citas externas y consistencia de marca.

Nota:
- No existe control total sobre lo que responden estos sistemas, pero sí se mejora mucho la probabilidad con contenido público, estructurado, enlazado y con autoridad.

---

## 5) KPIs y seguimiento (90 días)

KPIs técnicos:
- `% URLs válidas en sitemap` (objetivo: >98% 200/indexables).
- `% URLs con schema válido` (objetivo: >95%).
- `Tiempo de descubrimiento` desde publicación hasta indexación.

KPIs de visibilidad:
- Impresiones orgánicas en consultas con artista + localidad.
- Clics orgánicos a fichas de evento.
- Nº de URLs indexadas por tipo (hub/provincia/artista/evento).

KPIs de negocio:
- Tráfico orgánico a eventos.
- CTR desde SERP.
- Conversiones/acciones derivadas desde páginas de evento.

Cadencia:
- Revisión semanal (primer mes), quincenal (mes 2-3), mensual después.

---

## 6) Checklist de implementación inmediata

- [ ] Verificar en entorno publicado que `https://festia.es/robots.txt` devuelve `200`.
- [ ] Verificar que `https://festia.es/eventos` devuelve `200` sin autenticación.
- [ ] Enviar `https://festia.es/eventos/sitemap.xml` a Search Console.
- [ ] Añadir `h1` en listados de eventos.
- [ ] Eliminar meta description duplicada.
- [ ] Añadir enlaces HTML rastreables a provincia y artista.
- [ ] Revisar JSON-LD para evitar campos vacíos.
- [ ] Ejecutar validación de datos estructurados en muestra de URLs.

