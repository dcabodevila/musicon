# Recarga Automática de Permisos en Sesión Activa - Documentación

## Visión General

Esta implementación resuelve el problema de que los cambios en los permisos de un usuario con sesión activa no se reflejan inmediatamente en `SecurityContextHolder`. El caso principal es el onboarding de agencias, donde un usuario de tipo AGENCIA recibe permisos sobre su nueva agencia.

**Commit:** c5ccf6bb6cc4e5be41589e4fe45e8d4eecaec65c

## Cambios Implementados

### 1. WebSecurityConfig.java

**Agregado:** Bean `HttpSessionEventPublisher`

```java
@Bean
public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
}
```

**Propósito:** Sincroniza cambios de sesión en `SessionRegistry` publicando eventos HTTP de sesión en toda la aplicación. Es un requisito para que `SessionRegistry` reciba eventos de creación y destrucción de sesiones.

**Ubicación:** `src/main/java/es/musicalia/gestmusica/config/WebSecurityConfig.java` (líneas 58-60)

---

### 2. SecurityService.java (Interfaz)

**Métodos agregados:**

```java
void invalidarSesionDeUsuario(Long idUsuario);

void recargarOInvalidarSesion(Long idUsuarioAfectado);
```

**Propósitos:**
- `invalidarSesionDeUsuario()`: Invalida todas las sesiones activas de un usuario específico
- `recargarOInvalidarSesion()`: Método de conveniencia que decide automáticamente:
  - Si `idUsuarioAfectado` == usuario actual → recarga autoridades
  - Si `idUsuarioAfectado` != usuario actual → invalida su sesión

**Ubicación:** `src/main/java/es/musicalia/gestmusica/auth/model/SecurityService.java` (líneas 8-10)

---

### 3. SecurityServiceImpl.java (Implementación)

#### Inyecciones Agregadas

```java
@Autowired
private SessionRegistry sessionRegistry;
```

#### Método: invalidarSesionDeUsuario(Long idUsuario)

**Lógica:**
1. Valida que `idUsuario` no sea nulo
2. Itera sobre todos los `principals` en `sessionRegistry.getAllPrincipals()`
3. Para cada `principal` que sea instancia de `CustomAuthenticatedUser`:
   - Si el `userId` coincide, obtiene todas sus sesiones
   - Llama `session.expireNow()` en cada una
4. Loguea el número de sesiones invalidadas
5. Captura excepciones sin re-lanzar (no bloqueante)

**Logging:**
- INFO: Número de sesiones invalidadas
- WARN: Si `idUsuario` es nulo
- ERROR: Cualquier excepción durante la invalidación

**Ubicación:** Líneas 66-92

#### Método: recargarOInvalidarSesion(Long idUsuarioAfectado)

**Lógica:**
1. Valida que `idUsuarioAfectado` no sea nulo
2. Obtiene el usuario autenticado actual desde `userService.obtenerUsuarioAutenticado()`
3. Compara IDs:
   - **Si son iguales:** Llama `reloadUserAuthorities()` (recarga del usuario actual)
   - **Si son diferentes:** Llama `invalidarSesionDeUsuario()` (invalida al otro usuario)
4. Loguea la acción tomada
5. Captura excepciones sin re-lanzar

**Logging:**
- INFO: Acción ejecutada (recarga o invalidación)
- WARN: Si no hay usuario autenticado
- ERROR: Cualquier excepción

**Ubicación:** Líneas 94-122

---

### 4. AgenciasController.java

#### Inyección del Servicio

```java
private final SecurityService securityService;

public AgenciasController(..., SecurityService securityService) {
    ...
    this.securityService = securityService;
}
```

**Ubicación:** Líneas 38, 41, 47

#### Integración en guardarAgencia()

**Dónde se llama:** Tras guardar exitosamente la agencia (fuera del bloque try-catch de negocio)

**Código:**
```java
// Recarga/invalida sesión si se ha asignado un responsable
if (agenciaDto.getIdUsuario() != null) {
    try {
        securityService.recargarOInvalidarSesion(agenciaDto.getIdUsuario());
    } catch (Exception e) {
        log.error("Error recargando/invalidando sesión tras asignación de responsable: {}", e.getMessage());
    }
}
```

**Propósito:** Cuando se asigna una agencia a un usuario (acción de admin), su sesión se recarga o invalida automáticamente.

**Ubicación:** Líneas 110-117

#### Integración en guardarAgenciaOnboarding()

**Dónde se llama:** Tras crear exitosamente la agencia en onboarding (fuera del bloque try-catch de negocio)

**Código:**
```java
// Recarga permisos del usuario actual tras onboarding exitoso
try {
    securityService.reloadUserAuthorities();
} catch (Exception e) {
    log.error("Error recargando permisos tras onboarding: {}", e.getMessage());
}
```

**Propósito:** Cuando un usuario AGENCIA completa el onboarding, sus autoridades/permisos se recarga inmediatamente en la sesión actual.

**Ubicación:** Líneas 168-173

---

## Arquitectura y Flujos

### Flujo 1: Onboarding de Agencia (Usuario Actual)

```
1. Usuario AGENCIA accede a /agencia/onboarding
2. Completa formulario y POST a /agencia/onboarding
3. AgenciaServiceImpl.crearAgenciaOnboarding() crea la agencia
4. crearAgenciaOnboarding() llama a saveAgencia()
5. saveAgencia() crea Acceso (permisos sobre la agencia)
6. saveAgencia() retorna exitosamente
7. [En AgenciasController] securityService.reloadUserAuthorities()
8. SecurityServiceImpl.reloadUserAuthorities():
   - Obtiene usuario actual del contexto
   - Llama CustomUserDetailsServiceImpl.loadUserByUsername()
   - CustomUserDetailsServiceImpl recarga:
     * Roles generales desde BD
     * Maps de permisos (artista y agencia) desde permisoService
   - Crea nuevo CustomAuthenticatedUser con datos frescos
   - Reemplaza el token en SecurityContextHolder
9. Usuario ve sus nuevos permisos inmediatamente
```

**Mapas actualizados:**
- `mapPermisosAgencia`: Ahora incluye la nueva agencia con su ID y permisos
- `mapPermisosArtista`: Se refresca por si hay cambios

### Flujo 2: Asignación de Agencia a Usuario (Otro Usuario)

```
1. Admin edita una agencia en /agencia/edit/{id}
2. Asigna o cambia el responsable (idUsuario)
3. POST a /agencia/guardar
4. AgenciaServiceImpl.saveAgencia() guarda los cambios
5. saveAgencia() crea/actualiza Acceso si idUsuario cambió
6. saveAgencia() retorna exitosamente
7. [En AgenciasController] securityService.recargarOInvalidarSesion(idUsuario)
8. SecurityServiceImpl.recargarOInvalidarSesion():
   - Obtiene usuario actual (el admin que hace la asignación)
   - Compara: ¿idUsuario == admin? NO
   - Llama invalidarSesionDeUsuario(idUsuario)
9. InvalidarSesionDeUsuario():
   - Itera sobre sessionRegistry.getAllPrincipals()
   - Encuentra todas las sesiones del usuario afectado
   - Llama session.expireNow() en cada una
10. Usuario afectado es desconectado automáticamente
11. En su próximo request, es redirigido a login
12. Al re-autenticarse, sus nuevos permisos están disponibles
```

### Flujo 3: Recarga en Usuario Actual (Edge Case)

Si el admin cambia sus propios permisos (asigna su propia agencia):

```
1. Admin accede a /agencia/edit/{id}
2. Se asigna a sí mismo
3. POST a /agencia/guardar
4. [En AgenciasController] securityService.recargarOInvalidarSesion(idAdmin)
5. SecurityServiceImpl.recargarOInvalidarSesion():
   - Obtiene usuario actual (el mismo admin)
   - Compara: ¿idAdmin == admin? SÍ
   - Llama reloadUserAuthorities()
6. La sesión se mantiene, pero con permisos frescos
7. No hay desconexión
```

---

## Garantías de Diseño

### No-Bloqueante

Las llamadas a `securityService.reloadUserAuthorities()` / `recargarOInvalidarSesion()` están envueltas en try-catch sin re-lanzar. Esto garantiza que:

- Errores en recarga/invalidación NO revierten la acción de negocio ya confirmada
- Si la BD no responde en reloadUserAuthorities(), la agencia ya fue guardada
- El usuario puede intentar recargar la página manualmente si es necesario

**Logging:** Todos los errores se loguean a nivel ERROR para debugging

### Thread-Safe

- `SessionRegistry` es thread-safe
- `SecurityContextHolder` usa `ThreadLocal` (una copia por hilo)
- Cada usuario tiene su propio `SessionRegistry` entry
- Invalidar la sesión de un usuario no afecta otras sesiones

### Compatibilidad

- Usa APIs estándar de Spring Security (no hacks internos)
- `SessionRegistry` ya estaba en la aplicación
- `HttpSessionEventPublisher` es un bean estándar de Spring
- No modifica el modelo de datos ni el schema de BD

---

## Testing Recomendado

### Test Unitario (SecurityServiceImpl)

```java
@Test
void testInvalidarSesionDeUsuario() {
    // Mock sessionRegistry
    // Mock CustomAuthenticatedUser con userId=1
    // Añadir principal a sessionRegistry
    // Llamar invalidarSesionDeUsuario(1)
    // Verificar que session.expireNow() fue invocado
}

@Test
void testRecargarOInvalidarSesion_UsuarioActual() {
    // Mock SecurityContextHolder con usuario actual ID=1
    // Mock userService.obtenerUsuarioAutenticado() retorna usuario ID=1
    // Llamar recargarOInvalidarSesion(1)
    // Verificar que reloadUserAuthorities() fue invocado
    // Verificar que invalidarSesionDeUsuario() NO fue invocado
}

@Test
void testRecargarOInvalidarSesion_OtroUsuario() {
    // Mock SecurityContextHolder con usuario actual ID=1
    // Mock userService.obtenerUsuarioAutenticado() retorna usuario ID=1
    // Llamar recargarOInvalidarSesion(2)
    // Verificar que reloadUserAuthorities() NO fue invocado
    // Verificar que invalidarSesionDeUsuario(2) fue invocado
}
```

### Test de Integración (AgenciasController)

```java
@Test
void testGuardarAgenciaOnboarding_RecargaPermisos() {
    // Usuario AGENCIA con sesión activa
    // POST /agencia/onboarding con datos válidos
    // Verificar que agencia fue creada
    // Verificar que mapPermisosAgencia en CustomAuthenticatedUser incluye la nueva agencia
    // Verificar que puede acceder a /agencia/{id} sin "AGENCIA_EDITAR" error
}

@Test
void testGuardarAgencia_InvalidaSesionOtroUsuario() {
    // Admin con sesión activa
    // Usuario U1 con sesión activa
    // Admin POST /agencia/guardar asignando agencia a U1
    // Verificar que U1 es desconectado (su sessionId ya no es válido)
    // U1 intenta GET /agencia/mis-agencias → redirigido a login
}
```

---

## Archivos Modificados

| Archivo | Cambios |
|---------|---------|
| `WebSecurityConfig.java` | +6 líneas: Agregar bean `HttpSessionEventPublisher` e import |
| `SecurityService.java` | +4 líneas: Dos métodos en interfaz |
| `SecurityServiceImpl.java` | +65 líneas: Implementación de dos métodos, nuevo autowire + imports + Slf4j |
| `AgenciasController.java` | +29 líneas: Inyectar SecurityService, llamadas en dos métodos + import |
| **Total** | **104 líneas agregadas** |

---

## Notas Técnicas

### Diferencia: invalidarSesionDeUsuario vs reloadUserAuthorities

- **reloadUserAuthorities():** Mantiene la sesión del usuario actual. Sus autoridades en `SecurityContext` se recargan desde BD. Útil para cambios en permisos del usuario actual.

- **invalidarSesionDeUsuario():** Mata la sesión del usuario. La próxima request lo envía a login. Útil para cambios en permisos de **otro** usuario (fuerza a re-autenticarse para garantizar nuevos permisos).

### Por qué invalidar vs recargar para otros usuarios

- Si un admin asigna permisos a usuario U, no podemos simplemente recargar en `SecurityContextHolder` (que es del admin, no de U)
- Invalidar la sesión es la solución más segura: fuerza a U a re-autenticarse
- U tendrá una nueva sesión con permisos actualizados desde BD

### Logging

Todos los métodos loguean:
- **DEBUG:** Entrada/salida (se puede habilitar si es necesario)
- **INFO:** Acciones exitosas (recarga, invalidación, N° sesiones)
- **WARN:** Condiciones sospechosas (idUsuario nulo, usuario no autenticado)
- **ERROR:** Excepciones con contexto (para debugging y alertas)

**Recomendación:** Configurar logs en `application-{profile}.properties`:
```properties
logging.level.es.musicalia.gestmusica.auth.model.SecurityServiceImpl=INFO
```

---

## FAQ

**P: ¿Qué pasa si reloadUserAuthorities() falla?**
R: Se loguea como ERROR. La agencia ya fue guardada. El usuario puede recargar la página manualmente o limpiar caché.

**P: ¿Qué pasa si invalidarSesionDeUsuario() falla?**
R: Se loguea como ERROR. El usuario mantiene su sesión con permisos viejos. Será invalidado en el próximo polling o al expirar su sesión normal.

**P: ¿Por qué no usar `@Transactional` en los métodos de seguridad?**
R: No acceden a BD. Usan APIs en-memoria (SecurityContextHolder, SessionRegistry). No necesitan transacción.

**P: ¿Qué si SessionRegistry es null?**
R: El bean `HttpSessionEventPublisher` requiere `SessionRegistry`. Si no está inyectado, habrá `NullPointerException` en `invalidarSesionDeUsuario()`. El catch lo loguea. Por eso el bean `HttpSessionEventPublisher` es crítico.

**P: ¿Funciona con múltiples servidores (cluster)?**
R: No con esta implementación. `SessionRegistry` es en-memoria por JVM. En cluster necesitarías:
- SessionRegistry distribuido (p. ej., Redis)
- Pub/Sub de eventos entre servidores
- Hoy es monolito, así que está OK.

---

## Historial

| Fecha | Cambio |
|-------|--------|
| 2026-03-20 | Implementación inicial |
| | Commit: c5ccf6bb6cc4e5be41589e4fe45e8d4eecaec65c |
