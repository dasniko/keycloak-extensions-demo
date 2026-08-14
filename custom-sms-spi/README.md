# Keycloak Custom SMS SPI

Defines a brand-new, non-internal Keycloak SPI for sending SMS messages, plus a trivial logging implementation. This lets other modules (e.g. `authenticators`' `MfaAuthenticator`) depend on `SmsProvider` without caring which SMS gateway is actually behind it.

---

## The SPI (`SmsSpi`)

Registers a new SPI named `sms` with Keycloak (`@AutoService(Spi.class)`), tying together:

- **`SmsProvider`** — the runtime interface, a single method: `boolean sendMessage(String phoneNumber, String message)`.
- **`SmsProviderFactory`** — the factory interface (`init`/`postInit`/`close` all default to no-ops, so implementations only need to provide `create()` and `getId()`).

To add a real SMS gateway (Twilio, Vonage, AWS SNS, ...), implement both interfaces in a new package, annotate the factory with `@AutoService(SmsProviderFactory.class)`, and select it by provider ID wherever an `SmsProvider` is looked up (e.g. via the `provider` config property on `MfaAuthenticator` in the `authenticators` module).

---

## Logging SMS Provider (`logger`)

`LoggingSmsProvider` / `LoggingSmsProviderFactory` — the only implementation shipped in this module. Instead of sending anything, it just logs the phone number and message at `WARN` level and reports success. This is the default `SmsProvider` used by `MfaAuthenticator`, so the SMS-based MFA flow works out of the box in local/demo environments (e.g. via `docker compose up`) without needing a real SMS account — the "sent" code shows up in the Keycloak container logs.
