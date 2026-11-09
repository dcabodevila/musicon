# Feature: Upgrade Spring Boot 3.2.5 -> 3.5.x

Goal: move the platform to Spring Boot 3.5.x (Spring Security 6.5) as the base for a later passkeys (WebAuthn) feature.
Branch: `feature/upgrade-spring-boot-3.5`. No build; verification through focused `mvn test` runs (JDK 17).

## Specs
- S1: Spring Boot parent 3.2.5 -> 3.5.x (latest 3.5 patch: 3.5.16). Java 17 and `war` packaging unchanged.
- S2: springdoc-openapi 2.1.0 -> a 2.x release compatible with Spring Framework 6.2 (2.8.x+). "Subir a springdoc **2.7 o 2.8.x**. Es obligatorio."
- S3: Lombok updated to the latest 1.18.x.
- S4: Actuator properties: "Cambiarlas por `management.endpoints.access.default=none` y `management.endpoint.health.access=unrestricted`."
- S5: Fix whatever breaks at startup/tests, expected from Hibernate 6.6 ("Corregir lo que rompa al arrancar o en los tests, previsiblemente Hibernate 6.6").
- S6: `DaoAuthenticationProvider`: "Cambiarlo por `new DaoAuthenticationProvider(userDetailsService)`."
- S7: Tests: "Pasarlos a `@MockitoBean` / `@MockitoSpyBean`."
- S8: Verification: focused `mvn test` (no build), compared with the pre-upgrade baseline; startup against a dev DB, login, `/eventos/**` and Swagger need a manual/Playwright pass.

## Tasks
- T1 (S1-S4): pom versions + actuator properties. Route: inline. Commit: 3e2a6b9.
- T2 (S5): fix test/startup failures caused by the upgrade. Route: inline. Commit: n/a (nothing broke).
- T3 (S6-S7): deprecations (DaoAuthenticationProvider, MockitoBean). Route: inline. Commit: 31e05c4.
- T4 (S8): verification vs baseline + independent verify (Risk item 5) + RDD native review. Route: gentle-ai-verify + gentle_review. Commit: n/a.

## Log
- L1: User request (after the upgrade analysis): "ok" accepting "¿Quieres que lo implemente en una rama `feature/upgrade-spring-boot-3.5`?" with the plan: 1) parent 3.5.x, springdoc 2.8.x, Lombok, Actuator properties; 2) fix startup/test breakage (Hibernate 6.6); 3) DaoAuthenticationProvider + @MockBean -> @MockitoBean; 4) verify with focused tests, startup, login, /eventos/** and Swagger.
- L2: Target versions checked on Maven Central (2026-11-09): spring-boot-starter-parent 3.5.16; springdoc 2.x latest 2.10.0 (3.x is for Boot 4); lombok 1.18.48.
- L3: Baseline on main (Boot 3.2.5, JDK 17): 372 tests, 4 failures + 8 errors pre-existing (WebErrorPageMvcTest x8 context load, AuthControllerTest.login_conLogout_modelTieneMessage, EventoPublicoCatalogoFacadeImplTest quick links date, EventoPublicoControllerMvcTest.municipioPublico..., ListadoControllerIntegrationTest.generarListado_sinRango...). T1 commit 3e2a6b9: same 12, no new failures; resolved Boot 3.5.16 / Spring 6.2.19 / Security 6.5.11 / Hibernate 6.6.53 / springdoc 2.8.17.
- L4: T2: static review of DTO->entity save paths found no Hibernate 6.6 risk (mappers ignore id; toAcceso unused; primitive long ids treated as new at 0). No code change. Runtime check vs dev DB still pending (T4).
- L5: T3 applied and tested (same 12 baseline failures, none new). Commit blocked: GGA pre-commit hook (v2.8.1, opencode provider) fails with "Argument list too long" even for a single Java file. Not bypassed; awaiting user decision.
- L6: User: "no uses gga, desactívalo. En su lugar usa el agente verify y rdd". GGA pre-commit hook removed with `gga uninstall` (shared hooks dir of the clone). T3 committed as 31e05c4.
- L7: Verifier flagged that `./mvnw test` includes OcupacionNullMunicipioTest (@ActiveProfiles("dev"), local PostgreSQL localhost:5432/gestmusica_db) which saves ocupaciones. Parent's 3 runs executed it; methods are @Transactional with @Primary primaryTransactionManager, so the writes were rolled back (only possible residue: sequence increments). Verifier instructed to exclude it (-Dtest='!OcupacionNullMunicipioTest'). Correction: 15 test files migrated to @MockitoBean, not 16.
- L8: Incident (parent's instruction): `-Dtest='!OcupacionNullMunicipioTest'` makes Surefire also select ReactivacionTokenServiceIT (dev profile, local PostgreSQL). Verifier ran it once on candidate and on main: 2 tests pass in each run; each test creates a "react-token-<suffix>@test.local" user and deletes it in `finally` (ReactivacionTokenServiceIT.java:62,82), so expected residue is only sequence increments. OAuth calls to https://oauth.test/token (401) logged. Both copies: 372 tests, same 12 pre-existing failures (WebErrorPageMvcTest root cause: HomeController -> missing AgenciaPublicacionEventosService bean). No upgrade regression.
- L9: gentle-ai-verify verdict PARTIAL: S1-S7 met, no regressions (own runs on candidate and main: 372/4/8, same names); S8 manual pending; procedural incident L8. Verifier's proposed ActuatorAccessTest needed ApplicationConversionService to bind `unrestricted`; fixed and committed as d2bab82 (green; fails on access.default=unrestricted and on health.access=none). Verifier scratch path no longer present. RDD assess: high risk, reviewDue; inspect stopped with managed_assets_outdated; `gentle-ai sync --agent pi` reported nothing to update.
