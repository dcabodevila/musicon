# Plan de Cambios para Indexación SEO/IA de Eventos Públicos

## Objetivo
Mejorar la capacidad de Google y otros motores/IA para descubrir, interpretar e indexar correctamente las páginas de eventos públicos (`/eventos/**`), reduciendo señales ambiguas y contenido duplicado.

## Alcance
Este plan aplica a:
- Controlador público de eventos.
- DTO y generación de datos estructurados (JSON-LD).
- Plantillas Thymeleaf de listado y detalle.
- Descubrimiento técnico (`sitemap.xml`, `robots.txt`).

Archivos principales implicados:
- `src/main/java/es/musicalia/gestmusica/eventopublico/EventoPublicoController.java`
- `src/main/java/es/musicalia/gestmusica/eventopublico/EventoPublicoDto.java`
- `src/main/resources/templates/evento-publico.html`
- `src/main/resources/templates/eventos-publicos.html`
- `src/main/resources/templates/eventos-publicos-provincia.html`
- `src/main/resources/static/robots.txt` (nuevo)

---

## 1) Devolver 404 real cuando el evento no existe

### Por qué
Actualmente, si un evento no existe, se redirige a `/eventos`. Eso puede generar **soft-404** (Google ve una URL de detalle que responde con una página válida pero sin el contenido esperado). Esto perjudica calidad de indexación y cobertura.

### Cómo
- En `verEventoPublico(...)`, sustituir `return "redirect:/eventos";` por respuesta 404 real.
- Opciones técnicas:
  - Lanzar `ResponseStatusException(HttpStatus.NOT_FOUND, "...")`.
  - O devolver una vista de error 404 con código HTTP explícito.

### Validación
- `GET /eventos/evento/{id-inexistente}` debe devolver `HTTP 404`.
- En Search Console, disminución de URLs marcadas como soft-404 con el tiempo.

---

## 2) Corregir `sitemap.xml` para reflejar cambios reales

### Por qué
Se está usando `lastmod = LocalDate.now()` para todos los eventos. Eso envía una señal de actualización falsa diaria y provoca recrawls ineficientes.

### Cómo
- En cada `<url>`, usar la fecha real del evento o mejor aún la fecha de última modificación del registro (si existe campo `updatedAt`).
- Mantener solo URLs canónicas de detalle de evento.
- Si el volumen crece, crear `sitemap index` y partir por bloques.

### Validación
- Revisar `eventos/sitemap.xml`: `lastmod` distinto por evento cuando aplique.
- Search Console: mejor consistencia entre fecha rastreada y fecha de contenido.

---

## 3) Enriquecer y robustecer el JSON-LD (`MusicEvent`)

### Por qué
El marcado actual funciona como base, pero le faltan señales clave para máxima comprensión por buscadores y agentes IA:
- Fecha/hora sin zona horaria explícita.
- Faltan propiedades útiles (`url`, `image`, `organizer`, `offers` cuando aplique).
- Se construye JSON manualmente (más propenso a errores de escape).

### Cómo
- Construir el JSON-LD con objetos Java + serialización Jackson (evitar `StringBuilder` manual).
- Emitir `startDate` con offset (por ejemplo `Europe/Madrid`) en formato ISO-8601 con zona.
- Añadir propiedades recomendadas cuando existan datos:
  - `url`: URL canónica del evento.
  - `description`: texto limpio y útil.
  - `image`: imagen principal del evento/plataforma.
  - `organizer`: entidad organizadora.
  - `offers`: precio/moneda/disponibilidad si aplica.
- Mantener `eventStatus` y `eventAttendanceMode`.

### Validación
- Pasar la URL por Rich Results Test / validator.schema.org.
- Verificar que no haya warnings críticos en propiedades principales.

---

## 4) Eliminar riesgo de XSS/rotura en bloque JSON-LD

### Por qué
El uso de `th:utext` con JSON montado manualmente aumenta riesgo de inyección o cierre accidental del `<script>` (p. ej. `</script>` en texto).

### Cómo
- Generar JSON seguro mediante serializador.
- Mantener `type="application/ld+json"`, pero asegurar que el contenido ya está correctamente serializado y saneado.
- Evitar concatenaciones manuales de texto no confiable.

### Validación
- Tests con contenido especial (`"`, saltos de línea, secuencias HTML/script).
- El HTML resultante conserva estructura válida y sin ejecución inesperada.

---

## 5) Añadir URL canónica y estrategia anti-duplicado

### Por qué
Los listados con filtros (`desde`, `hasta`, `municipio`) pueden crear muchas variaciones de URL casi idénticas. Sin `canonical`, se diluye señal SEO.

### Cómo
- En detalle de evento: `<link rel="canonical" href="{url-canonica-evento}">`.
- En listados:
  - Definir canónica base (`/eventos`, `/eventos/provincia/{provincia}`) para combinaciones filtradas.
  - Evaluar `meta robots noindex,follow` en combinaciones muy específicas/no estratégicas.

### Validación
- Inspección de HTML en todas las variantes: canonical presente y consistente.
- Search Console: reducción de “Duplicada, Google eligió otra canónica”.

---

## 6) Crear `robots.txt` con referencia a sitemap

### Por qué
No hay evidencia de `robots.txt` propio. Es recomendable declarar reglas básicas y publicar el sitemap para acelerar descubrimiento.

### Cómo
Crear `src/main/resources/static/robots.txt` con contenido inicial:

```txt
User-agent: *
Allow: /

Sitemap: https://<dominio>/eventos/sitemap.xml
```

Ajustar dominio real de producción.

### Validación
- `GET /robots.txt` responde 200.
- Contiene la URL correcta del sitemap en entorno productivo.

---

## 7) Mejorar semántica SEO de URLs (slug)

### Por qué
Las URLs solo con ID (`/evento/{id}`) son válidas, pero menos descriptivas para usuarios, buscadores e IA. Un slug mejora CTR y contexto semántico.

### Cómo
- Extender ruta a formato estable: `/eventos/evento/{id}-{slug}`.
- Resolver siempre por `id` y usar `slug` como decorativo (si no coincide, redirección 301 al canónico).
- Generar slug con `nombreArtista + municipio + fecha` normalizado.

### Validación
- URL antigua debe seguir funcionando (compatibilidad).
- URL canónica con slug devuelve 200 y las variantes redirigen 301.

---

## 8) Corregir HTML inválido dentro de `<head>`

### Por qué
En plantillas hay un `<div>` dentro de `<head>`. Aunque navegadores lo toleran, no es HTML válido y puede afectar parseo de metadatos por crawlers.

### Cómo
- Reemplazar fragmento por inclusión compatible con `<head>` (sin `div` contenedor).
- Confirmar que meta tags y scripts SEO quedan en orden correcto dentro del `<head>`.

### Validación
- W3C validator sin errores estructurales en cabecera.
- Meta tags visibles en “ver código fuente”.

---

## 9) Mejorar señales de contenido principal en detalle

### Por qué
El título visual actual es genérico (“Detalles del evento”). Para SEO e IA conviene que el contenido principal tenga encabezado específico (entidad + lugar + fecha).

### Cómo
- Usar un `h1` descriptivo con artista/municipio/fecha.
- Mantener coherencia con `<title>` y `meta description`.

### Validación
- Cada evento muestra un `h1` único y descriptivo.
- Mejor alineación entre snippet y contenido visible.

---

## 10) Gobernanza de indexación para páginas sin valor de búsqueda

### Por qué
Páginas de resultado vacío o filtros extremadamente concretos pueden generar ruido en el índice.

### Cómo
- Definir reglas:
  - Indexar detalle de evento y listados principales.
  - Considerar `noindex,follow` para resultados vacíos o combinaciones no estratégicas.
- Añadir criterio en controlador/vista según tamaño del resultado.

### Validación
- Revisar cobertura de indexación: menos páginas de bajo valor, más foco en eventos reales.

---

## Orden recomendado de implementación
1. 404 real para evento inexistente.
2. Canonical en detalle y listados.
3. `robots.txt` + sitemap mejorado (`lastmod` real).
4. Refactor JSON-LD a serialización segura con zona horaria.
5. Slugs en URLs de evento.
6. Ajustes semánticos y estrategia de `noindex` en listados de baja calidad.

---

## Criterios de éxito global
- Cobertura de indexación más limpia en Google Search Console.
- Reducción de soft-404 y duplicados canónicos.
- Validación correcta de `MusicEvent` en herramientas de schema.
- Mayor consistencia entre URL, metadatos, contenido visible y sitemap.

---

## Notas de operación
- Ejecutar pruebas en entorno de staging antes de producción.
- Al desplegar, enviar/actualizar sitemap en Search Console.
- Monitorizar durante 2-6 semanas métricas de rastreo/indexación.
