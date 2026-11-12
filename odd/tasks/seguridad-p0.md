# Feature: Security P0 fixes (account takeover)

Goal: close the P0 findings of the security audit: brute-forceable verification codes, REGISTRO codes issued for already verified accounts, reusable password reset codes in the URL, and editing other users' profiles.
Base: `main` (8c41428). Branch `fix/seguridad-p0`. Inline implementation with test-first; independent verify (high-risk: security).

## Specs
- S1 (verification code attempts): User: "ponemos un límite de 5 intentos". Every verification code (REGISTRO and RECUPERACION_PASSWORD) accepts at most 5 failed attempts; on the 5th failure the code is invalidated and even the right code is then rejected. Generating a new code invalidates the previous active codes of the same email and type, so attempts cannot be multiplied by keeping several codes alive. The code stays 4 digits (the user chose the attempt limit). New column via manual script `sql/1.7.2/01_codigo_verificacion_intentos.sql` (AGENTS.md: never touch the local DB).
- S2 (REGISTRO only for pending accounts): audit proposal accepted with S1: "Emitir y aceptar códigos de registro solo para cuentas con el email sin verificar. No iniciar sesión automáticamente en cuentas que ya estaban activas." `POST /auth/resend-code` sends nothing for an unknown or already verified email (same response, no enumeration); `POST /auth/verify-email` rejects ("Código incorrecto o expirado.") and never activates nor logs in an account whose email is already verified.
- S3 (user edits): User: "corrígelo como indicas" on: "Tarea: usar el id del usuario autenticado, salvo que tenga el permiso `USUARIOS`. Proteger también `GET /usuarios/{id}` y `/usuarios/editar/{id}`." `POST /usuarios/guardar` and `GET /usuarios/editar/{id}` only for the own user or `USUARIOS`; otherwise 403 and nothing saved. `GET /usuarios/{id}` stays as is: it is the read-only profile linked from the `representantes.html` directory, and `usuario-edit.html` already hides email/phone unless `permisoVerDatosUsuario` (see L4).
- S4 (password reset): User: "ok" on: "Tarea: un token aleatorio de un solo uso, guardado como hash, enviado por POST y consumido en el paso final." The verified reset code is no longer in any URL; after `POST /auth/verify-reset-password` succeeds the grant lives server-side (HTTP session, never sent to the client), `GET /auth/reset-password` without a grant redirects to `/auth/remember-password`, `POST /auth/reset-password` takes the email from the grant (form email/codigo ignored) and consumes it, so a second POST cannot change the password again.

## Tasks
- T1 (S1): attempt limit + invalidate previous codes + SQL script. Route: inline, test-first. Commit: fa94adb.
- T2 (S2): REGISTRO resend/verify only for unverified accounts. Route: inline, test-first. Commit: dee7d9f.
- T3 (S3): UsuarioController ownership checks. Route: inline, test-first. Commit: 77263b4.
- T4 (S4): session-bound single-use reset grant. Route: gentle-ai-worker (context backstop reached), test-first. Commit: 6869ffa.
- T5: independent verify (gentle-ai-verify) + RDD assess. Route: subagent. Commit: n/a.

## Log
- L1: User: "quiero abordar una versión de mejora en la seguridad, analiza el código y dime las tareas que debo abordar antes"
- L2: User: "en la P0: 1. ponemos un límite de 5 intentos 2. corrgíelo como indicas 3. ok". P0 item 4 (rate limiter / X-Forwarded-For) not authorized: out of scope.
- L3: Engram unavailable in this session (runtime contract `engram.pi-http-runtime/v1` rejected): no mirror `odd/seguridad-p0/tasks`.
- L4: Decision on S3: restricting `GET /usuarios/{id}` would break the agencies' representantes directory, which links every representative's profile; the private data on that view is already gated by `permisoVerDatosUsuario`. Only edit and save are restricted.
- L5: Evidence T1+T2: RED observed (compilation: no attempt counter, no `isEmailPendienteVerificacion`); GREEN `./mvnw -q test -Dtest=CodigoVerificacionServiceTest,AuthControllerTest,UserServiceImplTest,RegistroUserServiceTest,RegistroControllerTest` all passing (90 tests + UsuarioControllerMvcTest RED for T3). Four characterization tests in RegistroControllerTest gained the new precondition (account pending verification).
- L6: Evidence T3: RED observed (UsuarioControllerMvcTest: other-user save not forbidden, other-user edit rendered); GREEN `./mvnw -q test -Dtest=UsuarioControllerMvcTest` EXIT=0. Existing save tests now authenticate as the edited user (id 1); the passkeys test editing another user uses a USUARIOS principal.
- L7: Evidence T4 (worker): RED 65 tests / 7 failures; GREEN `./mvnw -q test -Dtest=AuthControllerTest,RegistroControllerTest` EXIT=0, 65 tests (39 + 26). Behavior change for users: the reset step must happen in the same browser session that verified the code; otherwise it goes back to /auth/remember-password.
