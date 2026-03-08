# API de Eventos Públicos - Documentación

## Descripción

Sistema de publicación de eventos musicales con soporte para indexación de Google Events mediante Schema.org JSON-LD.

Esta implementación permite que las actuaciones confirmadas sean visibles públicamente en internet y puedan ser indexadas por Google, apareciendo en resultados de búsqueda cuando los usuarios busquen eventos musicales en una ubicación específica.

## Características

- ✅ **Schema.org JSON-LD** - Marcado estructurado compatible con Google Events
- ✅ **SEO optimizado** - Meta tags, Open Graph, títulos descriptivos
- ✅ **Sitemap XML** - Para facilitar la indexación de Google
- ✅ **API REST** - Endpoints JSON para integraciones externas
- ✅ **Responsive** - Diseño adaptado a móviles y desktop
- ✅ **Sin autenticación** - Páginas públicas accesibles para crawlers

## Endpoints disponibles

### Páginas web (HTML)

#### 1. Evento individual
```
GET /eventos/publicos/evento/{id}
```
Muestra la página de un evento específico con JSON-LD embebido para Google.

**Ejemplo:** `https://festia.es/eventos/publicos/evento/12345`

**Características:**
- JSON-LD Schema.org tipo `MusicEvent`
- Meta tags SEO completos
- Open Graph para redes sociales
- Información detallada del evento

#### 2. Eventos de un artista
```
GET /eventos/publicos/artista/{idArtista}
```
Lista todos los eventos futuros de un artista específico.

**Ejemplo:** `https://festia.es/eventos/publicos/artista/42`

#### 3. Listado general
```
GET /eventos/publicos
```
Muestra todos los eventos públicos futuros.

### API REST (JSON)

#### 4. API - Eventos por artista
```
GET /eventos/publicos/api/artista/{idArtista}
Content-Type: application/json
```

**Respuesta:**
```json
[
  {
    "id": 12345,
    "nombreArtista": "Orquesta París de Noia",
    "fecha": "2026-03-15T22:00:00",
    "lugar": "Plaza de la Fiesta",
    "municipio": "Pontevedra",
    "provincia": "Pontevedra",
    "matinal": false,
    "tarde": false,
    "noche": true,
    "informacionAdicional": "Fiesta de primavera"
  }
]
```

#### 5. API - Eventos por provincia
```
GET /eventos/publicos/api/provincia/{provincia}?desde=2026-03-01&hasta=2026-03-31
Content-Type: application/json
```

**Parámetros opcionales:**
- `desde` - Fecha desde (formato ISO: YYYY-MM-DD)
- `hasta` - Fecha hasta (formato ISO: YYYY-MM-DD)

#### 6. Sitemap XML
```
GET /eventos/publicos/sitemap.xml
Content-Type: application/xml
```

Genera automáticamente un sitemap con todas las URLs de eventos para Google.

## Schema.org JSON-LD

Cada página de evento incluye un JSON-LD con esta estructura:

```json
{
  "@context": "https://schema.org",
  "@type": "MusicEvent",
  "@id": "https://festia.es/eventos/publicos/evento/12345",
  "name": "Actuación de Orquesta París de Noia",
  "startDate": "2026-03-15T22:00:00",
  "location": {
    "@type": "Place",
    "name": "Plaza de la Fiesta",
    "address": {
      "@type": "PostalAddress",
      "addressLocality": "Pontevedra",
      "addressRegion": "Pontevedra",
      "addressCountry": "ES"
    }
  },
  "performer": {
    "@type": "MusicGroup",
    "name": "Orquesta París de Noia"
  },
  "eventStatus": "https://schema.org/EventScheduled",
  "eventAttendanceMode": "https://schema.org/OfflineEventAttendanceMode",
  "description": "Información adicional del evento"
}
```

## Reglas de publicación

Un evento es público y visible cuando cumple TODAS estas condiciones:

1. ✅ Estado = **OCUPADO** (ocupacionEstado.id = 1)
2. ✅ Activo = **true**
3. ✅ Fecha >= **HOY** (eventos futuros)

Los eventos con estado RESERVADO, PENDIENTE o ANULADO **NO** se publican.

## Configuración de seguridad

Los endpoints `/eventos/publicos/**` están configurados como públicos en `WebSecurityConfig.java`:

```java
.requestMatchers("/eventos/publicos/**").permitAll()
```

Esto permite que:
- Google y otros crawlers accedan sin autenticación
- Los usuarios vean eventos sin necesidad de login
- Las IAs (ChatGPT, etc.) puedan indexar la información

## Cómo usar

### Para mostrar eventos de un artista en su perfil

```html
<a href="/eventos/publicos/artista/{{ artistaId }}" target="_blank">
  Ver próximas actuaciones
</a>
```

### Para compartir un evento específico

```html
<a href="/eventos/publicos/evento/{{ ocupacionId }}" target="_blank">
  Ver detalles del evento
</a>
```

## Integración con Google

### 1. Enviar sitemap a Google Search Console

Una vez desplegado en producción:

1. Accede a [Google Search Console](https://search.google.com/search-console)
2. Añade tu dominio si no lo has hecho
3. Ve a **Sitemaps**
4. Añade la URL: `https://festia.es/eventos/publicos/sitemap.xml`

### 2. Validar marcado estructurado

Usa [Google Rich Results Test](https://search.google.com/test/rich-results) para validar:

```
https://festia.es/eventos/publicos/evento/12345
```

### 3. Monitorizar indexación

En Google Search Console, revisa:
- **Coverage** - Para ver qué páginas están indexadas
- **Enhancements > Events** - Para ver eventos detectados
- **Performance** - Para ver búsquedas que llevan a tus eventos

## Ejemplo de búsquedas que funcionarán

Una vez indexado, los usuarios podrán encontrar los eventos así:

- "orquestas que tocan este fin de semana en Pontevedra"
- "eventos musicales en Santiago de Compostela"
- "actuaciones Orquesta París de Noia"
- "fiestas en Galicia marzo 2026"

## Arquitectura

```
┌─────────────────────────────────────────────────┐
│           EventoPublicoController               │
│  - Maneja requests HTTP                         │
│  - Genera HTML con Thymeleaf                    │
│  - Responde JSON para API                       │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│         EventoPublicoService                    │
│  - Lógica de negocio                            │
│  - Filtra eventos públicos                      │
│  - Convierte Ocupacion → EventoPublicoDto       │
└──────────────────┬──────────────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────────────┐
│         OcupacionRepository (JPA)               │
│  - Acceso a base de datos                       │
│  - Queries con Specifications                   │
└─────────────────────────────────────────────────┘
```

## Modelos

### EventoPublicoDto

```java
{
  Long id;
  String nombreArtista;
  LocalDateTime fecha;
  String lugar;
  String municipio;
  String provincia;
  boolean matinal;
  boolean tarde;
  boolean noche;
  String informacionAdicional;
}
```

## Pruebas locales

Para probar en desarrollo:

1. Arrancar la aplicación
2. Acceder a: `http://localhost:8080/eventos/publicos`
3. Validar JSON-LD en: `https://validator.schema.org/`

## Notas importantes

- ⚠️ Los eventos solo se publican si están en estado **OCUPADO**
- ⚠️ Los eventos pasados no aparecen (fecha < HOY)
- ⚠️ El sitemap se regenera dinámicamente en cada request
- ⚠️ La indexación de Google puede tardar días o semanas
- ✅ Las URLs son permanentes y compartibles
- ✅ No se expone información privada (precios, usuarios, etc.)

## Mantenimiento

El sistema es automático:
- ✅ No requiere intervención manual
- ✅ Se actualiza en tiempo real con las ocupaciones
- ✅ Los cambios en ocupaciones se reflejan inmediatamente
- ✅ No hay caché (siempre datos actuales)

## Soporte

Para dudas o mejoras, contactar con el equipo de desarrollo.
