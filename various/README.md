# Keycloak Various Extensions

A grab-bag of smaller, unrelated extension examples that didn't warrant their own module: cache tuning, a client policy executor, a custom error response, and extra TOTP app suggestions.

---

## Custom Cache Configuration

`CustomCacheConfigProviderFactory` extends Keycloak's `DefaultCacheEmbeddedConfigProviderFactory` (the Infinispan embedded-cache config SPI) to register an additional named cache, `my-cache`, alongside Keycloak's built-in caches:

- `DIST_SYNC` with 2 owners when Keycloak is running clustered (transport configured); `LOCAL` otherwise.
- 5-second entry lifespan, capped at 10 entries in memory.

`order()` is bumped by 10 over the default factory so this one takes precedence. Once registered, any provider code can use the cache with:

```java
Cache<String, String> cache = session.getProvider(InfinispanConnectionProvider.class).getCache("my-cache");
String v = cache.computeIfAbsent("key", k -> "value");
```

Nothing else in this repo uses `my-cache` yet — it's a template for adding your own short-lived, size-bounded cache.

---

## Enforce `form_post` for Implicit/Hybrid Flow (`enforce-implicit-form-post`)

`EnforceImplicitFormPostExecutor` is a `ClientPolicyExecutorProvider` for use in a **Client Policy**: on every authorization request, if the requested `response_type` includes `token` (implicit or hybrid flow) and `response_mode` isn't `form_post`, the request is rejected with `responseModeNotAllowed` (400 Bad Request).

This is a common OAuth hardening measure — implicit/hybrid responses with `query` or `fragment` response modes can leak access tokens via browser history or `Referer` headers, while `form_post` delivers them in a POST body instead. Attach it to a client policy (with a client profile matching the clients you want to restrict) via the admin console or `kcadm`; no config properties of its own.

---

## Custom Error Handler

`CustomKeycloakErrorHandler` (`@Provider`, `@Priority(1)`) extends Keycloak's built-in `KeycloakErrorHandler` (a JAX-RS `ExceptionMapper`) with one special case: when the thrown exception is a `CustomKeycloakException`, it responds with `400 Bad Request` and the exception's message rendered directly as the HTML body — bypassing Keycloak's normal themed error page entirely. Every other exception type falls through to `super.toResponse()`, i.e. the regular Keycloak error handling.

Throw `CustomKeycloakException` from your own provider code wherever you want this minimal, unthemed error response instead of the standard error page.

---

## Custom OTP Application Suggestions (`authy`, `2fas`)

`AuthyOTPProvider` and `TwoFASOTPProvider` implement `OTPApplicationProviderFactory` to add [Authy](https://authy.com/) and [2FAS](https://2fas.com/) to the list of suggested authenticator apps Keycloak shows on the TOTP setup page, alongside the built-in FreeOTP/Google Authenticator suggestions.

Each declares `supports(OTPPolicy)` to only be suggested when the realm's OTP policy is compatible with that app:

| Provider | Requires |
|---|---|
| `authy` | `type=totp`, `period=30`, `digits=6`, `algorithm=HmacSHA1` |
| `2fas` | `type=totp`, `period=30`, `algorithm=HmacSHA1` |

Display names are resolved via message keys, with **two** keys per app because the same label needs translating in two different theme contexts: `totpAppAuthyName`/`totpApp2FASName` (account/login theme) and `otpSupportedApplications.totpAppAuthyName`/`otpSupportedApplications.totpApp2FASName` (admin theme) — see `messages_en.properties`.

---

## Notes

`src/main/java/org/keycloak/services/ErrorPage.java` shares its package and class name with Keycloak's own internal `org.keycloak.services.ErrorPage`, but it is **not** an unmodified copy — diffed against upstream, no version contains this file's `COOKIE_NOT_FOUND` special case (redirecting to the client's base URL instead of showing an error page) or its `resolveBaseUrl()` helper; conversely, it's missing the `session.getTransactionManager().setRollbackOnly()` call current upstream makes before rendering the error page. It also isn't referenced anywhere in this module, so none of this currently runs — but if it's ever wired in (or ends up shadowing the real class on the classpath), be aware it's a hand-modified variant, not a faithful copy of any Keycloak version checked.
