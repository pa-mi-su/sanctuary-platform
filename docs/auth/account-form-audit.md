# Account form lifecycle audit

## Findings and changes

| Area | Finding | Change |
| --- | --- | --- |
| iOS plain account inputs | Email, name and code fields had no explicit clear control. | Add labeled clear buttons with 44-point targets and return focus to the input. |
| iOS navigation | Back changed the step while retaining fields and pending reset/confirmation context. | Clear all fields, reveal flags and messages on Back/exit; invalidate abandoned requests. |
| Android state | Auth drafts, including passwords and codes, used `rememberSaveable`, which can serialize into saved-instance-state Bundles. | Use memory-only state for account drafts; clear on manual navigation, dispose pending context, and handle system Back. |
| Android requests | View-model requests could complete after the account form disappeared. | Cancel the active auth job and fence UI callbacks by form generation; check cancellation before session persistence. |
| Web account inputs | Passwords had clear buttons; plain inputs did not. | Add translated, field-specific clear labels and return input focus; clearing a password hides its value again. |
| Web navigation | Back retained values in the component; delayed responses could repopulate a changed flow. | Clear all drafts on Back/destruction/parent flow changes and ignore obsolete completions. |
| Confirmation | Registration passwords were retained to automatically sign in after code confirmation. | Discard passwords when advancing to code entry; existing confirmation flow falls back to the login prompt. |
| Current web admin | Shared input component already provides clear/reveal controls; restart/destroy clears auth state and uses a generation guard. | Retain existing behavior; existing admin tests passed. |

Target email is retained only while moving forward into its active confirmation/reset flow. Back abandons that context. Validation errors within an active form still allow correction without discarding the entire form. Password-manager/autofill support remains enabled; an OS/browser may offer saved credentials independently of app form state.

No password or code persistence was added. Existing iOS Keychain, Android encrypted session preferences and browser session handling remain unchanged. Android network logging uses BASIC rather than request-body logging. No AWS, database, account or MFA settings were changed. This audit covers account input/navigation behavior, not a claim of a complete application security assessment.

## Verification

- iOS DEV simulator build passed.
- Android DEV APK build and all 16 existing unit tests passed.
- Web development build and all 37 tests passed. Five new account tests cover accessible clearing, repeated Back navigation, target-email handoff and late responses after destruction/flow changes.
- In an isolated iPhone simulator, verified email clear, Login → Back → Login, and Forgot Password → Back → Forgot Password. Reopened fields were empty. Synthetic email only; no login, registration, reset-code or password-change request submitted.
- [Reset-email clear-button preview](reset-email-clear.png).
- Android UI behavior and other native forms share the audited input/navigation code; they were not all individually exercised on physical phones in this pass.
