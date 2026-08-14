# Keycloak Extensions - Shared Utilities

This module is not a deployable Keycloak provider itself. It's a shared library of helper classes and test infrastructure used by the other extension modules in this repo, pulled in as a `provided`-scope Maven dependency.

---

## Test Infrastructure (`de.keycloak.test`)

- **`TestBase`** — the common base class most module tests extend (see each module's `README`/tests). Built on `testcontainers-keycloak`, it provides:
  - `requestToken(...)` — several overloads to run a Resource Owner Password Credentials grant against a running `KeycloakContainer`, defaulting to the `admin-cli` client.
  - `parseToken(String)` — decodes a JWT's payload (base64) into a `Map`, without verifying its signature.
  - `getUser(...)` / `updateUser(...)` — admin-client helpers to look up a user by username and update it via the Admin REST API.
  - `initTestRealm(...)` — creates a test realm, with optional customizer callbacks for the realm representation before/after creation.
- **`AuthorizationCodeGrant`** — a standalone helper (not part of `TestBase`) that drives a full Authorization Code + PKCE flow with REST-assured against the real browser login form. Deliberately limited: it only works with the default browser authentication flow (no OTP or other flow variants) and the default theme, since it locates the login form's POST URL by parsing the rendered HTML for the `kc-form-login` element ID.
- **`de.keycloak.test.pages`** — a small HtmlUnit-based page-object model for tests that need to drive an actual browser session rather than raw HTTP:
  - `AbstractPage` — base class handling page-load verification and typed transitions between pages.
  - `LoginWithUsernameAndPasswordPage`, `UpdatePasswordPage`, `AccountManagementPage` — page objects for the corresponding built-in Keycloak pages.

---

## General Utilities (`de.keycloak.util`)

| Class | Purpose |
|---|---|
| `AuthenticatorUtil` | `getConfig(context, key, defaultValue)` reads a value from an `Authenticator`'s per-execution `AuthenticatorConfigModel`, type-converting it (`String`/`Boolean`/`Long`/`Integer`) to match the given default, or returning the default if unset. Used by the `authenticators` module (e.g. `MfaAuthenticator`, `DeviceCookieAuthenticator`). |
| `ComponentUtil` | Looks up a realm's `ComponentModel`(s) for a given provider class and provider ID — e.g. to read a component's own admin-configured settings from provider code. |
| `TokenUtils` | Generates access tokens outside the normal login flow: `getServiceAccountToken()` (cached and auto-refreshed per realm+client, for machine-to-machine calls) and several `generateAccessToken()` overloads for minting a token for an arbitrary user/session without them actually logging in. |
| `BuildDetails` | Reads `git.properties` (produced by the `git-commit-id-maven-plugin`) to expose branch, commit and build-time info — falls back to the current UTC time and the exception message if the file isn't present. |
| `AuthenticationMethodReference` | Enum of the [RFC 8176](https://www.rfc-editor.org/rfc/rfc8176.html) Authentication Method Reference values (`pwd`, `otp`, `sms`, `mfa`, ...) for use in an `amr` claim. |
| `ThrowingConsumer<T, E>` / `ThrowingFunction<T, R, E>` | `Consumer`/`Function` variants whose single method is allowed to declare a checked exception `E`, for use in lambdas that call checked-exception-throwing code. |
| `TriConsumer<T, U, V>` | A three-argument `Consumer` (package-private). |

---

## Provider Helpers (`de.keycloak.provider`)

**`DefaultServerInfoAware`** — a default-method mixin for `ServerInfoAwareProviderFactory`. Implement this interface (instead of `ServerInfoAwareProviderFactory` directly) on any provider factory to automatically expose `BuildDetails` for that class in the admin console's Server Info page, without writing `getOperationalInfo()` yourself.

---

## Annotations (`de.keycloak.annotation`)

**`@CopiedFromKeycloak`** — a source-only (`RetentionPolicy.SOURCE`) documentation annotation for marking code that was copied from a specific Keycloak class rather than reused as a dependency (e.g. because the original is `private`/internal). Carries fields for the original `source()` class, the Keycloak `version()` it was copied from, whether it was `modified()` and how (`changes()`), why it had to be copied instead of reused (`reason()`), and an optional `copiedAt()` date. Not currently applied anywhere in this repo, but available for the next time copy-pasted Keycloak internals show up in a module.
