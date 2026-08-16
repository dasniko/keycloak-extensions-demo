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

## Trusted Device Cookie (`device-cookie`)

`DeviceCookieAuthenticator` recognizes a previously confirmed browser/device via a cookie. If the cookie is present and valid for the current user, the authenticator succeeds immediately. Otherwise, the user is emailed a one-time confirmation code (and link); only after confirming is the device trusted and remembered.

> Must run **after** an execution that resolves the user (e.g. `Username Password Form`) — `requiresUser()` returns `true`, so it needs `context.getUser()` to already be set.

### Cookie & device recognition

- Cookie name: `KC_DEVICE`. Value: `<deviceId>.<secret>` — two independent random alphanumeric strings (`deviceId`: 20 chars, `secret`: 32 chars), generated via `SecretGenerator`.
- Matched against the user's multi-valued `trusted_devices` attribute. Each entry has the form `<deviceId>:<sha256(secret)>:<expiresAt>`.
	- Only the **SHA-256 hash** of the secret is stored, never the secret itself — so reading a user's attributes (admin console/API) can't be used to forge a valid cookie. Comparison uses `MessageDigest.isEqual` (constant-time).
	- `expiresAt` is a server-side Unix timestamp, set to match the cookie's `Max-Age` at issuance time, and checked on every login. This closes a gap that a client-only `Max-Age` can't: a non-browser client could otherwise replay a captured cookie value forever, ignoring the browser-enforced expiry.
	- A user can have **multiple** trusted devices at once (phone, laptop, ...) — confirming a new one doesn't invalidate the others.

### Confirmation flow (cookie missing or invalid)

1. A numeric code is generated (`SecretGenerator`, digits only, length configurable) and stored purely as an **ephemeral auth-session note** (never persisted to the user) together with its own expiry.
2. It's emailed via `EmailTemplateProvider`. The email contains both the raw code, for manual entry, and a link back to the current authentication step (`getRefreshExecutionUrl()`) with that same code as a query parameter — clicking the link achieves the same result as typing the code, since both use one shared code rather than a separate "magic key".
3. Manual entry submits through `action()` (form POST); clicking the link re-enters `authenticate()` (GET) and detects the code in the query string. Both paths are verified by the same `verifyCode()` method.
4. The instant a submitted code matches, the auth-session notes are deleted — **before** even checking expiry — so a code or link can never be reused a second time, regardless of outcome.
5. A wrong code follows the same required/conditional/alternative retry semantics as `MfaAuthenticator`: re-challenge with an error if the execution is `REQUIRED`, otherwise `context.attempted()`. A correct-but-expired code is a hard failure — the login must be restarted for a new one.
6. Once verified, the device is added to `trusted_devices`, the cookie is set on the response, and the authenticator succeeds.

### Keeping `trusted_devices` bounded

Nothing ever runs on a timer to clean up the attribute. Instead, every time a device is (re)confirmed, expired entries are pruned from the list in the same write that appends the new one — since that's the *only* code path that ever writes the attribute, an actively-used account's device list stays bounded to roughly what was confirmed within one cookie lifetime. Accounts that stop logging in entirely just keep their last, now-stale, entries — harmless, since `isTrustedDevice()` already rejects anything past its `expiresAt`.

### Configuration

| Property | Config key | Default | Description |
|---|---|---|---|
| Cookie lifetime (seconds) | `cookieMaxAge` | `2592000` (30 days) | How long a confirmed device is remembered — used for both the cookie's `Max-Age` and the matching server-side `expiresAt`. |
| Confirmation code length | `codeLength` | `6` | Number of digits of the emailed confirmation code. |
| Confirmation code time-to-live (seconds) | `codeTtl` | `300` (5 minutes) | How long the emailed code/link stays valid. |

### Cookie attributes

Path is scoped to the realm (`AuthenticationManager.getRealmCookiePath`), `HttpOnly`, `SameSite=Lax`, and `Secure` is auto-detected via Keycloak's `SecureContextResolver` — so it still works over plain HTTP on `localhost` (e.g. the `docker compose` dev setup) without needing TLS.

### Templates & messages

- `device-confirm.ftl` — the login-form challenge page (code input field).
- `device-confirm-email.ftl` (in both `templates/html/` and `templates/text/`) — the confirmation email.
- Message keys: `deviceConfirmTitle`, `deviceConfirmText`, `deviceConfirmCodeLabel`, `deviceConfirmSubmit`, `deviceConfirmCodeInvalid`, `deviceConfirmCodeExpired`, `deviceConfirmEmailSubject`, `deviceConfirmEmailBody(Html)` — defined in `messages_en.properties` / `messages_de.properties`.

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
