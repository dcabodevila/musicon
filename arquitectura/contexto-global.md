# Musicon — Contexto Global de Arquitectura

## 1) Resumen ejecutivo

`musicon` es una aplicación monolítica en **Spring Boot** orientada a la gestión operativa de artistas, agencias, ocupaciones y eventos públicos, con un enfoque mixto de:

- **Backoffice server-side** (Thymeleaf + MVC clásico)
- **APIs REST puntuales** para integraciones y exposición pública

El sistema prioriza una arquitectura por dominios funcionales con separación por capas y una fuerte dependencia en integraciones externas (mail, assets, sincronización de eventos).

---

## 2) Stack tecnológico principal

- **Framework:** Spring Boot 3.2.5
- **Seguridad:** Spring Security
- **Persistencia:** Spring Data JPA + Hibernate
- **Base de datos principal:** PostgreSQL
- **Base legacy opcional:** MariaDB
- **Vistas:** Thymeleaf
- **Caching:** EhCache
- **Mapeo DTO/Entidad:** MapStruct
- **Reportes:** JasperReports
- **Storage media/documentos:** Cloudinary
- **Email transaccional:** Mailgun

---

## 3) Arquitectura lógica

Patrón dominante: **MVC en capas**.

Flujo típico:

1. `Controller` recibe request
2. `Service` aplica reglas de negocio
3. `Repository` ejecuta acceso a datos

La base de código está organizada por módulos de dominio (`artista`, `agencia`, `ocupacion`, `usuario`, `eventopublico`, etc.), y cada módulo tiende a contener:

- entidad
- DTO(s)
- repository
- service
- controller

Esto favorece mantenibilidad por dominio, aunque requiere consistencia fuerte en mapeos y contratos entre capas.

---

## 4) Persistencia y modelo de datos

### 4.1 Fuente principal (PostgreSQL)

- Siempre activa
- Gestiona entidades bajo `es.musicalia.gestmusica.*`

### 4.2 Fuente secundaria (MariaDB legacy)

- Activación condicional por `MARIADB_ENABLED=true`
- `EntityManagerFactory` y `TransactionManager` propios
- Entidades legacy bajo `es.musicalia.gestmusicalegacy.*`

### 4.3 Migraciones

- Hibernate en modo `validate` (no crea ni migra tablas automáticamente)
- Scripts SQL versionados en `/sql` (`*-ddl.sql` y `*-dml.sql`)

**Implicación:** los cambios de esquema dependen de disciplina operativa en scripts y despliegue.

---

## 5) Seguridad

Configuración destacada en `WebSecurityConfig` con dos cadenas de filtros:

1. **Cadena pública** para `/eventos/**` (acceso abierto + rate limiting)
2. **Cadena autenticada** para el resto (form login, límite de sesión concurrente, permisos custom)

Controles relevantes:

- Password hashing con BCrypt
- Evaluación de permisos con `CustomPermissionEvaluator`

---

## 6) Integraciones externas

- **Cloudinary:** almacenamiento de ficheros e imágenes
- **Mailgun:** envío de correo transaccional
- **Orquestas de Galicia API:** sincronización de eventos
- **JasperReports:** generación/exportación de PDF

La salud del sistema depende parcialmente de la disponibilidad y latencia de estos servicios.

---

## 7) Eventos públicos (módulo `eventopublico`)

Existe una API pública para descubrimiento de eventos con marcado **Schema.org JSON-LD** para indexación (SEO).

Reglas funcionales principales:

- Publicación condicionada a estado `OCUPADO`
- Fecha del evento en futuro

Referencia funcional: `doc/EVENTOS_PUBLICOS_API.md`.

---

## 8) Jobs programados

Hay tareas scheduladas con cron y diferencias por perfil (`dev`/`prod`), incluyendo:

- sincronización periódica
- sincronización diaria con orquestasdegalicia
- reporte mensual de agencia

Esto introduce requisitos de observabilidad operativa y alertado para detectar fallas silenciosas.

---

## 9) Hallazgos recientes relevantes (estado funcional)

En trabajo reciente del proyecto se observaron/mejoraron áreas de:

- flujo de baja/reactivación de email
- robustez transaccional en persistencia de token
- endpoint manual para disparar reactivación
- riesgos funcionales y deuda técnica en módulo de contratos

Estos puntos sugieren priorizar validación de flujos críticos y cobertura de integración en módulos sensibles.

---

## 10) Riesgos arquitectónicos y operativos

1. **Dependencia en integraciones externas** (email/API/assets) sin observabilidad suficiente puede degradar UX sin detección temprana.
2. **Dual datasource** aumenta complejidad transaccional y de configuración.
3. **Monolito por dominios** escala bien al inicio, pero necesita disciplina para evitar acoplamientos cruzados.
4. **Migraciones manuales SQL** requieren control estricto para evitar drift entre entornos.

---

## 11) Recomendaciones de siguiente nivel

1. Definir y monitorear SLOs por flujo crítico (registro, login, eventos públicos, envío de correo).
2. Implementar observabilidad por capas:
   - negocio (KPIs)
   - aplicación (latencia/error rate)
   - infraestructura (JVM/DB pool)
3. Fortalecer tests de integración para casos transaccionales e integraciones externas.
4. Documentar decisiones de arquitectura activas (ADR livianas por módulo crítico).

---

## 12) Archivos clave para profundizar

- `src/main/java/es/musicalia/gestmusica/config/DatabaseConfig.java`
- `src/main/java/es/musicalia/gestmusica/config/WebSecurityConfig.java`
- `src/main/resources/ehcache.xml`
- `doc/EVENTOS_PUBLICOS_API.md`
- `/sql/*` (versionado de esquema y datos)
