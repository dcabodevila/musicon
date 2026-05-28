---
name: playwright-browser-testing
description: "Trigger: Playwright, navegador, browser test, e2e, probar en el navegador. Run real-browser checks and capture user-visible results."
license: Apache-2.0
metadata:
  author: gentleman-programming
  version: "1.0"
---

# Playwright Browser Testing

## Activation Contract

Use this skill when the user asks to verify behaviour in a real browser, with Playwright, E2E, UI flow, screenshots, network responses, or “what the user sees”.

## Hard Rules

- Do not replace browser verification with raw HTTP unless the user explicitly accepts it.
- Keep tests focused on the reported behaviour; do not run broad suites or builds.
- Do not commit credentials, traces, screenshots, videos, or temporary scripts.
- Use temporary files under `C:\Users\Sir Nolimit\AppData\Local\Temp\opencode` when a project has no Playwright setup.
- For this project, run Playwright headed by default (`headless: false`) so the user can watch the browser. Use headless only if the user explicitly asks.
- Capture both machine evidence and human-visible evidence: status code/body plus screenshot or visible toast text.

## Decision Gates

| Situation | Action |
| --- | --- |
| Project has Playwright config | Add/run a focused spec using existing scripts. |
| No Playwright config | Create a throwaway temp npm folder/script and run Playwright there. |
| Login required | Authenticate through the UI unless the user specifically asks for storage state/session injection. |
| Need to bypass front validation but see UI response | Use the browser page context to call the same AJAX/helper path after filling required non-target fields. |
| Server returns file/blob | Inspect response headers/body and also assert whether the UI shows success/error notification. |

## Execution Steps

1. Confirm the app URL, credentials if needed, and the exact user-visible behaviour to prove.
2. Check for existing Playwright setup (`playwright.config.*`, package scripts) before creating temp files.
3. Open the browser with Playwright, authenticate via the login page, and navigate like a user.
4. If bypassing client validation, do it from `page.evaluate(...)` after the page loads so cookies, CSRF, and app JS are real.
5. Attach response listeners for the target endpoint and collect: URL, status, content type, body/JSON, console errors.
6. Capture visible page evidence: toast/alert text, button state, URL, and screenshot path.
7. Report only the focused outcome and cleanup/identify temp artifacts.

## Output Contract

Return:
- Browser used and mode (`chromium`, headed by default unless explicitly requested otherwise).
- Flow executed, including how front validation was bypassed.
- Target network response: status, content type, and relevant body.
- User-visible result: toast/message/text plus screenshot path if captured.
- Any limitation or reason the check could not be completed.

## References

- Playwright CLI/docs: https://playwright.dev/docs/intro
