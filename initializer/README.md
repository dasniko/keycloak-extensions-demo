# Keycloak Initializer

Defines a private, internal Keycloak SPI (`initializer`, `isInternal() == true`) purely for providers that do their work as a side effect of `postInit()` — none of them expose a runtime `Provider` instance (`InitializerProviderFactory.create()` defaults to `null`). Because the SPI is internal, its providers aren't configurable from the admin console; they're configured via `keycloak.conf` / `KC_SPI_INITIALIZER_*` environment variables instead.

All three providers below are independent and unrelated except for sharing this SPI — enable only the ones you need.

---

## Issuer Base URI Override (`issuer`)

`IssuerInitializerProvider` pins the OIDC issuer URL to a fixed, configured value instead of Keycloak's normal hostname-based resolution.

**How it works:** this isn't a config toggle Keycloak exposes — there's no extension point for overriding `realmIssuer()`. So `postInit()` uses [ByteBuddy](https://bytebuddy.net/) to redefine `org.keycloak.services.Urls#realmIssuer` at runtime (via a Java agent installed with `ByteBuddyAgent.install()`), delegating the redefined method to this class's own `realmIssuer(URI, String)`, which substitutes the configured base URI before delegating back to the original logic. This is bytecode instrumentation of Keycloak's own class, not a supported SPI hook — fragile across Keycloak versions by design, and the reason `byte-buddy`/`byte-buddy-agent` need `compile` (not `provided`) scope in `pom.xml`, since the agent classes must be present at runtime.

**Configuration:**

| Property | Env var | Description |
|---|---|---|
| `base-uri` | `KC_SPI_INITIALIZER_ISSUER_BASE_URI` | Fixed base URI to use as the issuer for every realm. Leave empty/unset to fall back to Keycloak's regular hostname-based issuer. |

Covered by `InitiailizerTest`, which starts a real container with and without the env var set and asserts the resulting `issuer` in the OIDC discovery document.

---

## Realm Post-Create Initializer (`kcInitializer`)

`RealmInitializerProvider` listens for `RealmModel.RealmPostCreateEvent` (registered on the `KeycloakSessionFactory` in `postInit()`) and runs setup logic whenever a **new realm** is created (skipping `master`).

Currently, it calls `AuthenticationFlows.createAuthFlows()`, which builds a demo **`browser-step-up`** top-level authentication flow if one doesn't already exist:

- Alternative executions: the built-in cookie authenticator, then the IdP authenticator.
- A "forms" alternative sub-flow containing two **conditional** branches, both gated by `ConditionalLoaAuthenticatorFactory` (Level of Authentication):
  - `basic / silver condition` — LoA level `1`, max-age `36000`s, satisfied by `Username Password Form`.
  - `advanced / gold condition` — LoA level `2`, max-age `10`s, satisfied by `OTP Form`.

This demonstrates building an authentication flow entirely from Java at realm-creation time (`RealmModel.addAuthenticationFlow` / `addAuthenticatorExecution` / `addAuthenticatorConfig`) rather than via `kcadm`/an import JSON file. There's also a commented-out snippet showing how you'd create and assign a default realm role (`user`) the same way — inactive, left as a template.

No configuration.

---

## Cron-Based Timer (`timer`)

Keycloak's own `TimerProvider` SPI only supports fixed-interval scheduling, not cron expressions. `AbstractCronProvider` adds cron support on top of it using the [`cron-utils`](https://github.com/jmrozanec/cron-utils) library: it parses a UNIX cron expression, computes the next execution time, and schedules a recurring 30-second poll (`TimerProvider.schedule`) that fires `run(session)` once the wall-clock time crosses that next-execution boundary — then recomputes the next one.

`ScheduledCronProvider` (`timer`) is the demo implementation: `run()` just logs that the timer fired.

> This is a different mechanism from the `scheduled-task` module's `ScheduledTaskProvider` SPI, which is interval-based (via Keycloak's `TimerProvider.scheduleTask`), not cron-expression-based. Use this one if you need cron syntax; use `scheduled-task` if a fixed interval is enough.

**Configuration:**

| Property | Env var | Default | Description |
|---|---|---|---|
| `enabled` | `KC_SPI_INITIALIZER_TIMER_ENABLED` | `false` | Whether the cron scheduler runs at all. |
| `cron-expression` | `KC_SPI_INITIALIZER_TIMER_CRON_EXPRESSION` | `*/1 * * * *` | UNIX cron expression (every minute, by default). |

To reuse this for a real scheduled job, extend `AbstractCronProvider` and implement `run(KeycloakSession)` instead of modifying `ScheduledCronProvider` directly.
