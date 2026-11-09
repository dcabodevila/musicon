# Feature: Passkeys (fingerprint / Face ID), remember me and saved passwords on login

Goal: let users sign in with a passkey (WebAuthn: fingerprint, Face ID, Windows Hello), stay signed in for 30 days, and let the phone's password manager save and fill the password.
Branch: `feature/passkeys-login`, chained on `feature/upgrade-spring-boot-3.5` (needs Spring Security 6.5). No build; focused tests only; DB changes as manual scripts under `sql/1.7.0/`.

## Specs
- S1 (remember me): "«Recordarme» (no volver a entrar)": checkbox on the login form; when checked, the session survives closing the browser or phone. Duration: "30 días, mantener 1 sesión" (keep `maximumSessions(1)`).
- S2 (saved password): "Que el móvil guarde la contraseña": login fields marked so iOS/Android/Chrome password managers save and fill them (`autocomplete="username"` / `current-password`).
- S3 (passkey login): "huella o Face ID" sign-in from the login page using WebAuthn passkeys (one implementation covers fingerprint, Face ID, Touch ID, Windows Hello).
- S4 (passkey enrolment offer): "Oferta tras el login + perfil": after a password login on a compatible device, offer "¿Quieres entrar con huella o Face ID la próxima vez?".
- S5 (passkey management): on the user's own profile (`/usuarios/mi-perfil`) list, add and delete passkeys/devices.
- S6 (constraints): keep the existing login flow working (password login, error, logout, expired session, functional event tracking); `/eventos/**`, `/baja/**`, `robots.txt` untouched; DB changes only as scripts in `sql/1.7.0/`.

## Design notes
- Principal name today is "nombre apellidos" (`CustomAuthenticatedUser.java:25-29`); remember me and WebAuthn reload the user by principal name, so it must become the login `username`; the navbar (`fragments/main-template.html:137`) keeps showing the full name.
- Remember me: `TokenBasedRememberMeServices` (SHA-256), 30 days, key from `app.security.remember-me.key=${REMEMBER_ME_KEY}` (prod has no default). Not the persistent-token variant: token rotation breaks with parallel requests (service worker), see Log.
- WebAuthn: `http.webAuthn()` with rpId/allowedOrigins from properties; credentials stored in `gestmusica` tables via JPA-backed repositories (fits Entity/Repository architecture; `ddl-auto=validate` means the SQL script must run before deploy).

## Tasks
- T1 (S6, S1 prerequisite): principal name = login username; navbar shows full name. Route: inline. Commit: 9b0a947.
- T2 (S1, S2): remember me + autocomplete on login form. Route: inline. Commit: 7027766.
- T3 (S3, S6): WebAuthn config, dependency, entities/repositories, SQL script `sql/1.7.0/`. Route: inline. Commit: pending.
- T4 (S3): login page passkey button + conditional UI JS. Route: inline. Commit: pending.
- T5 (S4, S5): post-login offer + profile management UI. Route: inline. Commit: pending.
- T6: independent verify (Risk items 1, 2, 3) + RDD review. Route: gentle-ai-verify + gentle_review. Commit: n/a.

## Log
- L1: User: "vamos a retomar el tema de la autenticación con huella , el remember password y el face id". Earlier context: "Me gustaría poder desbloquearlo con huella o cara en el móvil. Es posible eso en una webapp ?¿"
- L2: User answers: remember password = «Recordarme» (no volver a entrar) + Que el móvil guarde la contraseña; duration = 30 días, mantener 1 sesión; passkey enrolment = Oferta tras el login + perfil; branch = Rama encadenada (from feature/upgrade-spring-boot-3.5).
- L3: Exploration: UserDetailsService accepts username or email (`CustomUserDetailsServiceImpl.java:43-46`) and updates last access on every load (`:64-68`); principal name is full name (`CustomAuthenticatedUser.java:25-29`), also used as SessionRegistry identity (two users with the same full name share the max-1-session slot today). `FunctionalLoginSuccessHandler` always redirects to `/` and tracks `source=form_login`. Own profile: `GET /usuarios/mi-perfil` -> `usuario-detail-edit.html`. CSRF metas in `header-css` (`main-template.html:22-23`), fetch integration in `static/js/comun.js`. Service worker passes all fetches through. No JdbcTemplate bean; webauthn4j not managed by Boot 3.5.16 BOM (explicit version needed). login.html uses `header-css`, no autocomplete, no page JS.
- L4: User: "y un usuario que entre desde un dispositivo con face id puede entrar después en otro distinto?" Answered: synced passkeys (iCloud Keychain, Google Password Manager, 1Password/Bitwarden), cross-device QR (hybrid) handled by the browser, several passkeys per user, password always available. Design adjustment for S4 (applied unless the user objects): offer after every password login on a compatible device; stop offering on that device only after "Ahora no" or after creating a passkey from it (browser-local flag), not when the user already has any passkey.
- L5: T1: RED observed (3 failures: username was "Juan " / full name). Navbar via `sec:authentication="principal.nombreCompleto"` broke MVC tests whose principal is a plain `User`; then `${#authentication.name}` broke tests with no security context. Final: model attribute `nombreUsuarioAutenticado` from GlobalControllerAdvice with `sec:authentication="name"` fallback.
- L6: Slice boundary: the passkeys PR is chained on feature/upgrade-spring-boot-3.5 (base 69dcd64); its commit counter starts there, not at origin/main.
- L7: T2: RED observed (4 remember-me tests failing, 3 existing-behaviour tests passing). Test gotcha: the UserDetailsService mock must return a fresh User per call (Spring erases credentials after login and TokenBasedRememberMeServices reloads the user to sign the cookie); a failed login sends a cancel cookie (Max-Age=0). GREEN: 7/7; suite 372 tests, same 12 baseline failures. Prod requires REMEMBER_ME_KEY (application-prod.properties); dev/test fall back to a random key per start with a WARN.
- L8: Slice 2 (passkeys PR, chained on slice 1 = T1+T2 up to 7027766) starts with T3.
- L9: T3: webauthn4j-core 0.29.7.RELEASE (version pinned by Spring Security 6.5.11's libs.versions.toml; not in Boot BOM). JPA-backed adapters `PasskeyUserEntityRepository` (returns the user's current login name, so a username change does not break passkeys) and `PasskeyCredentialRepository`; tables `gestmusica.webauthn_usuario` / `gestmusica.webauthn_credencial` in `sql/1.7.0/01_webauthn_passkeys.sql` (validated on a throwaway PostgreSQL 16 cluster: idempotent, cascade delete, column types). RED observed (compile + 2 security tests); GREEN 13 adapter tests + 5 security tests. Contract learned: anonymous POST /webauthn/register/options answers 400 (Spring's filter), not a login redirect. Passkey login goes through the same UserDetailsService as password login (same active-user rules; by username it does not filter inactive users, pre-existing behaviour, unchanged).
- L10: T3 suite: 390 tests, same 12 baseline failures, none new.
