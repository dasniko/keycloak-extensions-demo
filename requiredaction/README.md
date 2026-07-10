# Keycloak Required Action Extensions

This module bundles several custom `RequiredActionProvider` implementations — some are standalone demo actions, others extend and customize Keycloak's built-in required actions (password update, TOTP setup, recovery codes).

[![](http://img.youtube.com/vi/KXZ9JDcSHU0/hqdefault.jpg)](http://www.youtube.com/watch?v=KXZ9JDcSHU0 "")

---

## Update Phone Number (`phone-number`)

`PhoneNumberRequiredAction` — asks the user for a mobile phone number and stores it in the `phone_number` user attribute.

It's an [initiated action](https://www.keycloak.org/docs/latest/server_admin/#initiating-required-actions), so it can be triggered on demand via `kc_action=phone-number` in the login/account URL, and can also be assigned to a user directly.

`evaluateTriggers()` is intentionally left empty, but contains a commented-out example of how self-registration would work (adding the action automatically whenever the user has no phone number set) — enable it if you want the action to auto-trigger.

The form performs minimal validation (non-blank, minimum length 5) before saving the number and completing the action.

---

## MFA Enrollment (`mfaEnrollment`)

`MfaEnrollmentAction` presents the user with a choice of MFA methods (e.g. OTP, WebAuthn) and lets them pick one to configure, instead of forcing a specific one.

**How it works:**
- `evaluateTriggers()` adds itself to the auth session if the user has none of the configured credential types set up yet, and no related required action is already pending.
- `requiredActionChallenge()` renders `mfa-enrollment-action.ftl`, listing one button per configured credential type/required action.
- `processAction()` reads the choice from the submitted form (`requiredActionName`) and adds *that* required action to the session, then removes itself — the chosen action (e.g. `CONFIGURE_TOTP`) takes over from there.

**Configuration:**

| Property | Description |
|---|---|
| Required Actions | Which required action provider IDs are offered as MFA options (e.g. `CONFIGURE_TOTP`, `CONFIGURE_RECOVERY_AUTHN_CODES`, `webauthn-register`). Defaults to TOTP + WebAuthn. |
| Conditional Role | If set, this action only applies to users who have (or, if negated, don't have) the given **realm** role. |
| Negate Role Condition | Inverts the role check above. |

Only providers implementing `CredentialRegistrator` are eligible as options, since the action needs to know which credential type each choice registers (see `RequiredActionUtil` below).

---

## Custom Update Password

`CustomUpdatePassword` extends the built-in `UPDATE_PASSWORD` action to optionally block password changes for users who log in via a federated identity provider.

**Configuration:**

| Property | Description |
|---|---|
| Disallowed for IdPs | List of identity providers for which password updates are forbidden. If the user has a federated identity with one of these IdPs, both the challenge and the submit are rejected with a `credentialUpdateNotAllowed` error (HTTP 403). Empty list = no restriction. |

Runs at `order() + 10` relative to the built-in action so it takes precedence.

---

## Custom Recovery Authentication Codes

`CustomRecoveryAuthnCodesAction` extends Keycloak's built-in `RecoveryAuthnCodesAction` so that recovery codes are automatically required once the user has configured *another* MFA credential (TOTP or WebAuthn) but has no recovery codes yet.

**Configuration:**

| Property | Description |
|---|---|
| Required Actions | Which credential-registering actions, when configured by the user, should trigger recovery-code enrollment (`CONFIGURE_TOTP`, `webauthn-register`). If left empty, the auto-trigger is skipped entirely. |

Shares the credential-type resolution logic (and its per-request cache) with `MfaEnrollmentAction` via `RequiredActionUtil`.

---

## Skippable Configure TOTP

`SkippableConfigureTOTP` extends the built-in `CONFIGURE_TOTP` action to allow a grace period during which users can skip/cancel OTP enrollment, e.g. while rolling out a new MFA requirement gradually.

**Configuration:**

| Property | Description |
|---|---|
| Skip Deadline | ISO-8601 date (e.g. `2025-01-01`). Before this date, the action is skippable/cancelable; from this date onward it's enforced and can no longer be skipped. Leave blank to always enforce it. |

Before the deadline, `requiredActionChallenge()` marks the action as cancelable by setting the `KC_ACTION_EXECUTING` / `KC_ACTION_ENFORCED` client notes on the auth session — this is the same mechanism Keycloak uses for `kc_action`-initiated actions, and it makes the UI show a "cancel" option. `initiatedActionCanceled()` then removes the required action if the user cancels.

Covered by `SkippableConfigureTOTPTest`, which tests the deadline logic (`isFutureDeadline`) in isolation for past, future, blank, and missing config values.

---

## Shared internals

These are not required actions themselves, but helpers used by the ones above.

- **`RequiredActionUtil`** — parses the multi-valued `requiredActions` config property shared by `MfaEnrollmentAction` and `CustomRecoveryAuthnCodesAction`, and resolves it to a map of credential type → provider ID by looking up which configured providers implement `CredentialRegistrator`. The result is cached per request in the `KeycloakSession` attributes to avoid recomputing it multiple times during one evaluation.
- **`CancelableAction`** — a default-method interface providing the "make this required action cancelable via `KC_ACTION_EXECUTING`/`KC_ACTION_ENFORCED`" pattern (the same one `SkippableConfigureTOTP` implements manually) as reusable building blocks: `setActionCancelable()`, `removeActionCancelable()`, `initiatedActionCanceled()`. It distinguishes an action that was triggered by the current auth flow (step-up, not yet on the user's persistent required-action list) from one that's already a pending, mandatory action on the account. Not yet wired into any provider in this module — a foundation for future cancelable actions.
