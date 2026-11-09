# Pruebas e2e con Playwright

Red de seguridad en navegador real sobre los flujos principales de la aplicación: login, registro, listados, ocupaciones y tarifas. Se ejecuta contra la **app dev ya arrancada** con la base de datos local; no arranca nada por sí misma.

## Requisitos

- Node.js 20.12 o superior.
- La aplicación arrancada en `http://localhost:8081` (o la URL que indiques en `E2E_BASE_URL`).
- Un usuario de pruebas activo y, para ocupaciones y tarifas, un artista de pruebas sobre el que ese usuario tenga los permisos `OCUPACIONES`, `ANULAR_OCUPACION` y `CREAR_TARIFAS`.

## Configuración

1. Instala las dependencias y el navegador (solo Chromium):

   ```bash
   cd e2e
   npm install
   npx playwright install chromium
   ```

2. Crea `e2e/.env` (está ignorado por git; nunca subas credenciales) con este contenido:

   ```dotenv
   E2E_BASE_URL=http://localhost:8081
   E2E_USER=
   E2E_PASSWORD=
   E2E_ARTISTA_ID=
   E2E_HEADLESS=
   ```

   | Variable | Uso |
   | --- | --- |
   | `E2E_BASE_URL` | URL de la app. Por defecto `http://localhost:8081`. |
   | `E2E_USER` / `E2E_PASSWORD` | Usuario E2E (nombre de usuario o email). Sin ellos, las pruebas que necesitan sesión se marcan como *skipped* con un mensaje claro. |
   | `E2E_ARTISTA_ID` | Artista de pruebas para ocupaciones y tarifas. Sin él, esos specs se omiten. |
   | `E2E_HEADLESS` | `1` para ejecutar sin ventana. Por defecto se ve el navegador (*headed*). |

   Las variables definidas en la shell tienen prioridad sobre `e2e/.env`.

## Ejecución

```bash
cd e2e
npx playwright test                 # toda la suite, navegador visible
npx playwright test tests/login.spec.ts
npx playwright test --list          # lista las pruebas sin ejecutarlas
npx tsc --noEmit                    # comprobación de tipos
npx playwright show-report          # informe HTML de la última ejecución
```

Sin ventana: `E2E_HEADLESS=1 npx playwright test` (bash) o `$env:E2E_HEADLESS='1'; npx playwright test` (PowerShell).

## Cómo está organizada

La app permite **una sola sesión por usuario** (`maximumSessions(1)`), así que los proyectos se ejecutan en orden y con un único *worker*:

1. `setup` (`tests/auth.setup.ts`): inicia sesión por la UI y guarda la sesión en `e2e/.auth/user.json`.
2. `autenticado`: listados, ocupaciones, tarifas y sincronización reutilizan esa sesión.
3. `anonimo`: login y registro, al final, porque sus inicios de sesión invalidan la sesión guardada.

Ejecutar la suite cierra cualquier otra sesión abierta del usuario E2E. El login limita a 10 intentos por ventana de tiempo; una ejecución completa usa 4.

| Spec | Qué comprueba |
| --- | --- |
| `login.spec.ts` | Redirección al login sin sesión, contraseña incorrecta, login correcto y cierre de sesión. |
| `registro.spec.ts` | Validación del navegador con campos vacíos y rechazos del servidor (contraseñas distintas, usuario existente). No crea usuarios. |
| `listados.spec.ts` | Aviso sin fechas sin llamar al servidor; generación con filtros y descarga del PDF. |
| `ocupaciones.spec.ts` | Crear, editar y anular una ocupación `E2E-` en 2098. |
| `tarifas.spec.ts` | Crear, editar y eliminar tarifas de un rango de 3 días en 2099. |
| `sincronizacion.spec.ts` | Omitida: la sincronización legacy no tiene pantalla en la UI (solo API `/api/sincronizacion` y job programado); está cubierta por tests unitarios. |

### Decisiones relevantes

- **Registro completo no automatizado**: tras registrarse, la cuenta no puede entrar hasta introducir el código enviado por email en `/auth/verify-email`, se enviaría un correo real y la UI no permite borrar usuarios.
- **Fechas reservadas**: ocupaciones en 2098 y tarifas en 2099, para no tocar datos reales. Tarifas: la prueba se detiene si algún día del rango ya tiene tarifa, para no sobrescribirla.
- **Eliminar tarifas de un rango**: en la UI, el botón *Eliminar* solo aparece al pulsar una tarifa existente en el calendario, inalcanzable en 2099 sin avanzar cientos de meses. La prueba muestra ese mismo botón en el modal abierto y confirma el diálogo, que ejecuta la función de la propia página.
- **Fechas en flatpickr**: se fijan con la instancia de flatpickr que crea la página, igual que al elegir un día en el calendario.
- **Efectos laterales**: anular una ocupación genera un mensaje interno y un email al usuario de la ocupación (el usuario E2E). Si el usuario E2E no tiene permiso `CONFIRMAR_OCUPACION` en la agencia, guardar una ocupación también avisa al responsable de la agencia; usa una agencia y un artista de pruebas.

## Limpieza de datos

Todo lo que crea la suite lleva el prefijo `E2E-` y se limpia por la UI cuando es posible (`afterAll` anula la ocupación o desactiva las tarifas si una prueba falla a medias). Lo que la UI no puede borrar se elimina con el script manual [`sql/1.7.0/02_e2e_cleanup.sql`](../sql/1.7.0/02_e2e_cleanup.sql):

- ocupaciones anuladas `E2E-`, sus mensajes internos y la tarifa que generaron;
- listados generados con *Solicitado para* `E2E-`.

Las tarifas desactivadas de 2099 no llevan texto propio; el script incluye un borrado opcional comentado que debes revisar antes de ejecutar. El script nunca se ejecuta desde la suite.

## Artefactos

`node_modules/`, `.env`, `.auth/`, `test-results/` y `playwright-report/` están ignorados por git. Las trazas y capturas se guardan solo cuando una prueba falla; no las subas al repositorio.
