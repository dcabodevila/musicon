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
- T1 (S1-S4): pom versions + actuator properties. Route: inline. Commit: pending.
- T2 (S5): fix test/startup failures caused by the upgrade. Route: inline. Commit: pending.
- T3 (S6-S7): deprecations (DaoAuthenticationProvider, MockitoBean). Route: inline. Commit: pending.
- T4 (S8): verification vs baseline + independent verify (Risk item 5). Route: gentle-ai-verify. Commit: n/a.

## Log
- L1: User request (after the upgrade analysis): "ok" accepting "¿Quieres que lo implemente en una rama `feature/upgrade-spring-boot-3.5`?" with the plan: 1) parent 3.5.x, springdoc 2.8.x, Lombok, Actuator properties; 2) fix startup/test breakage (Hibernate 6.6); 3) DaoAuthenticationProvider + @MockBean -> @MockitoBean; 4) verify with focused tests, startup, login, /eventos/** and Swagger.
- L2: Target versions checked on Maven Central (2026-11-09): spring-boot-starter-parent 3.5.16; springdoc 2.x latest 2.10.0 (3.x is for Boot 4); lombok 1.18.48.
