# Keycloak Authenticators

This module bundles several custom `Authenticator`/`FormAuthenticator`/`FormAction` implementations, each demonstrating a different authentication-flow use case — from simple gates (captcha, email domain restriction) to a full email-verified "trusted device" mechanism.

---

## Automatically Override Existing IdP Link (`idp-override-existing-link`)

`IdpOverrideExistingLinkAuthenticator` extends Keycloak's built-in `IdpConfirmOverrideLinkAuthenticator`.

Normally, when a user authenticates via a broker (IdP) whose identity is already linked to a *different* local account, Keycloak shows a confirmation page before overriding that link. This authenticator skips the confirmation: whenever an existing federated identity is found for the broker, it sets the `OVERRIDE_LINK` auth note and immediately succeeds.

No configuration.

---

## Captcha Authenticator (`captcha`)

`CaptchaAuthenticator` is a minimal math captcha: two random numbers are shown, and the user must submit their sum before the flow continues.

**Configuration:**

| Property | Description |
|---|---|
| Lower Bound / Upper Bound | Range for the two random operands. Configured per authenticator execution in the admin console. Server-level defaults can be preset via SPI config (`spi-authenticator-captcha-lower-bound` / `-upper-bound` in `keycloak.conf`), read in `init(Config.Scope)`. |

---

## Deny / Block Authenticators (`custom-deny-access`, `do-not-login`)

Two independent, always-failing authenticators in the `deny` package, for different situations:

- **`CustomDenyAccess`** (`custom-deny-access`) — always fails with a 401 **error** page and logs an `ACCESS_DENIED` event (reason `migrated_user`). Useful e.g. when the user has already been migrated to Keycloak and this login path should no longer be used for them.
- **`DoNotLogin`** (`do-not-login`) — always fails too, but with an **info** page instead of an error. If a user was already resolved by an earlier step, it disables that user and clears them from the flow context. Intended for cases like "user just self-registered but must not be automatically logged in" — an info page is friendlier here than an error.

Neither is configurable.

---

## Email Domain Regex Authenticator (`email-domain-regex-authenticator`)

`EmailDomainAuthenticator` rejects login if the resolved user's email doesn't match a configured regular expression (case-insensitive) — e.g. to restrict access to specific corporate domains.

If the regex or the user's email is missing, login is rejected with a `BAD_REQUEST` error page as a fail-safe, rather than silently letting everyone through on misconfiguration.

**Configuration:**

| Property | Description |
|---|---|
| Allowed Email Regex | Regular expression the user's email must fully match, e.g. `^.*@(firma\.de\|partner\.com)$`. |
| Fehlermeldung | Custom error message shown when the email doesn't match. |

---

## MFA via SMS Code (`custom-mfa`)

`MfaAuthenticator` sends a numeric one-time code to the user's mobile phone via the pluggable `SmsProvider` SPI (see the `custom-sms-spi` module), then validates it against what the user enters.

Requires the `mobile_number` user attribute; if missing, `setRequiredActions()` adds the `requiredaction` module's `PhoneNumberRequiredAction` so the user is asked for it first.

**Configuration:**

| Property | Description | Default |
|---|---|---|
| Code length | Number of digits of the generated code. | `6` |
| Time-to-live | How long the code is valid, in seconds. | `300` |
| Provider ID | ID of the `SmsProvider` implementation to use. | The logging/no-op SMS provider, which just prints the code — handy for local testing without a real SMS gateway. |

---

## MFA Enrollment (`mfa-enrollment`)

`MfaEnrollmentAuthenticator` lets the user pick which MFA method to set up, instead of forcing one specific one — modeled after [an upstream Keycloak PR (#17494)](https://github.com/keycloak/keycloak/pull/17494).

**How it works:**
- Presents one button per configured required-action provider ID (default: TOTP + WebAuthn).
- On submit, adds the chosen required action to the auth session, which then takes over the enrollment itself.

**Configuration:**

| Property | Description |
|---|---|
| Required Actions | Which required-action provider IDs are offered as MFA options. |
| Credential Types | The credential types those actions register, used to determine whether the user already has *any* of them configured (in which case this step is skipped). |

> See also the `requiredaction` module's own `MfaEnrollmentAction` — a similar idea, but implemented as a post-login *required action* rather than a flow-step authenticator. The two are independent; pick whichever fits where in the login lifecycle you need the choice to appear.

---

## Username/Password Form with Registration Redirect (`auth-usr-pwd-form-w-redirect`)

`UsernamePasswordWithRegistrationRedirectForm` extends the built-in `UsernamePasswordForm`. If the submitted username doesn't resolve to an existing user, instead of failing with "invalid credentials" it redirects to the realm's registration page (preserving the client ID, tab ID and client data as query parameters), so an unrecognized visitor lands on sign-up rather than an error.

Drop-in replacement for the standard "Username Password Form" execution in a browser flow.

---

## Custom Registration Page + Registration Code (`custom-registration-page`, `registration-code`)

A pair of providers demonstrating a custom registration form with an extra field:

- **`CustomRegistrationPage`** (`FormAuthenticator`) renders `registration-code.ftl`, pre-filling a `registrationCode` field — either a demo default (`4711`) or a value passed via a `registrationCode` query parameter on the registration URL.
- **`RegistrationCode`** (`FormAction`) validates that a non-blank code was submitted, and stores it as an auth-session note (`registrationCode`) for later use. The demo doesn't act on it further (see the comment in `success()`) — this is where real redemption logic (invite codes, referral codes, ...) would hook in.

Both must be added together to the same registration flow, replacing the standard "Registration Page" / "Registration User Creation" pairing.

---

## Shared internals

- **`AuthenticatorUtil.getConfig`** (from the `utils` module) — a small helper used by `MfaAuthenticator` and `DeviceCookieAuthenticator` to read and type-convert a per-execution `AuthenticatorConfigModel` value, falling back to a default when unset.
