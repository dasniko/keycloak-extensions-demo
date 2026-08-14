# Keycloak Action Tokens

Custom `ActionTokenHandler` implementations plus an admin REST endpoint to mint them — for generating one-time (or reusable) sign-in links that an external system can hand to a user, without going through Keycloak's own email delivery.

---

## Custom Action Token (`custom-action`) + Admin Endpoint (`custom-token`)

This is the main feature: an admin-authenticated REST endpoint that creates a link which, when opened, logs the given user directly into a client's required-actions flow — no password, no email round-trip through Keycloak.

### `POST /admin/realms/{realm}/custom-token`

Registered via `CustomTokenResourceProvider` (`AdminRealmResourceProviderFactory`), so it lives under the realm's admin REST API and is protected by normal admin permissions — the handler explicitly requires `manage-users`.

**Request body** (`CustomTokenRequest`):

| Field | Description |
|---|---|
| `email` / `username` | Identifies the target user (username takes precedence and disables `forceCreate` if both are set). |
| `clientId` | The client the resulting session/redirect is for. |
| `redirectUri` | Where to send the user after required actions complete; must pass `RedirectUtils.verifyRedirectUri` for the client. |
| `forceCreate` | If the user doesn't exist, create them (disabled automatically when `username` is used instead of `email`). |
| `updateProfile` / `updatePassword` | When creating a new user, add `UPDATE_PROFILE` / `UPDATE_PASSWORD` as required actions. |
| `scope`, `nonce`, `state` | Passed straight through to the OIDC session once the token is redeemed. |
| `expirationSeconds` | How long the link is valid for; defaults to 24 hours. |
| `reuse` | Whether the same link can be redeemed more than once (`canUseTokenRepeatedly`). |

**Response** (`CustomTokenResponse`): the (found-or-created) `userId`, and `link` — the full action-token URL to hand to the user via whatever channel you like.

Not allowed for the `master`/admin realm — `createTokenLink` throws if called against it, since a token like this is considered too high-risk for the realm that manages Keycloak itself.

### What happens when the link is opened

`CustomActionTokenHandler` (registered for the `custom-action` token type) handles the redeemed token:

- Starts a fresh authentication session for the token's client (`startFreshAuthenticationSession`).
- Restores the redirect URI, scope, `state` and `nonce` onto that session, exactly as if the user had started a normal OIDC authorization request.
- Marks the user's email as verified.
- Hands off to `AuthenticationManager.nextRequiredAction(...)` — so if the user has pending required actions (e.g. `UPDATE_PASSWORD` from account creation above), they see those first; otherwise they're redirected straight to `redirectUri`.

---

## Custom Reset-Credentials Handler

`CustomResetCredentialsActionTokenHandler` extends Keycloak's built-in `ResetCredentialsActionTokenHandler` (the handler behind the "forgot password" email link) with one behavioral tweak: it sets the `END_AFTER_REQUIRED_ACTIONS` auth note before delegating to the built-in logic, so that once the user finishes their required actions (e.g. setting a new password), the flow simply **ends** there instead of continuing on to a full client login/redirect.

`order()` is bumped one above the built-in handler's so this one takes precedence for the same token type.
