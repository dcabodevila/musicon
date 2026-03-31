# CSRF Unified Solution — Test Checklist

## Purpose
Validate that the global CSRF approach (meta tags in shared Thymeleaf layout + global JS helper for jQuery/fetch) works for authenticated write actions, preserves expected behavior for public endpoints, and does not alter requests that should remain untouched.

## Happy Path (manual)

1. **Release notes mark-read POST**
   - Log in with a valid user.
   - Open release notes UI and trigger "mark as read".
   - Confirm request in browser devtools.
   - **Expected:** POST returns 2xx (no 403), UI updates correctly.

2. **Typical authenticated AJAX POST (jQuery)**
   - From an authenticated page using jQuery AJAX, trigger a POST action.
   - Verify request headers in devtools.
   - **Expected:** CSRF header is present and request succeeds (2xx/expected app response), no 403.

3. **Typical authenticated fetch POST**
   - From an authenticated page using `fetch`, trigger a POST action.
   - Verify request headers in devtools.
   - **Expected:** CSRF header is present and request succeeds (2xx/expected app response), no 403.

## CSRF Coverage Cases

1. **Same-origin write methods auto-header**
   - Trigger same-origin `POST`, `PUT`, `PATCH`, `DELETE` via jQuery/fetch helper.
   - **Expected:** CSRF header is automatically added to each write request.

2. **Safe methods do not include CSRF header**
   - Trigger same-origin `GET`, `HEAD`, `OPTIONS` via jQuery/fetch helper.
   - **Expected:** No CSRF header is added.

3. **Cross-origin requests are untouched**
   - Trigger a request to a different origin from page JS (if CORS setup allows test call).
   - **Expected:** Helper does not inject CSRF header; request remains unmodified by CSRF logic.

4. **Shared layout pages include CSRF meta tags**
   - Open pages that use the shared Thymeleaf layout.
   - Inspect page `<head>`.
   - **Expected:** CSRF meta tags exist and contain values.

5. **Non-shared-layout pages are explicitly covered**
   - Identify pages not using the shared layout.
   - Verify they are either:
     - excluded from flows requiring CSRF token injection, or
     - provided explicit CSRF handling.
   - **Expected:** No broken authenticated write flow due to missing meta tags.

## Regression Cases

1. **Public endpoints keep expected behavior**
   - Exercise representative flows under `/eventos/**` and `/baja/**`.
   - **Expected:** Endpoints still behave as intended (no unintended CSRF-related breakage).

2. **Thymeleaf form submissions still work**
   - Submit representative server-rendered forms.
   - **Expected:** Form posts succeed with normal behavior.

3. **Logout still works**
   - Trigger logout from authenticated session.
   - **Expected:** Logout succeeds and redirects/invalidates session as expected.

## Compact Checklist Table

| Area | Case | How to test | Expected result |
|---|---|---|---|
| Happy path | Release notes mark-read | Trigger mark-read POST while authenticated | 2xx, no 403, UI reflects read state |
| Happy path | jQuery AJAX POST | Run authenticated jQuery POST, inspect headers | CSRF header present; request succeeds |
| Happy path | Fetch POST | Run authenticated fetch POST, inspect headers | CSRF header present; request succeeds |
| Coverage | Same-origin write methods | Test POST/PUT/PATCH/DELETE | Header auto-injected |
| Coverage | Safe methods | Test GET/HEAD/OPTIONS | Header not injected |
| Coverage | Cross-origin calls | Call different origin from JS | Request not modified by CSRF helper |
| Coverage | Shared layout meta tags | Inspect `<head>` on shared-layout pages | CSRF meta tags present with values |
| Coverage | Non-shared-layout pages | Validate exclusion/explicit handling | No missing-token breakage |
| Regression | Public endpoints | Verify `/eventos/**` and `/baja/**` flows | Behavior unchanged |
| Regression | Thymeleaf forms | Submit representative forms | Submission works as before |
| Regression | Logout | Trigger logout | Session closes and expected redirect |
