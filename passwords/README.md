# Keycloak Password Extensions

Password-related extensions: two additional password policies, a legacy hash algorithm for migrated credentials, and a credential provider that retroactively enforces password policy changes on existing users.

---

## Retroactive Password Policy Enforcement

Normally, a realm's password policy is only checked when a password is *set* — tightening the policy later doesn't affect users who already have a password that no longer conforms. `CustomPasswordCredentialProvider` (extending the built-in `PasswordCredentialProvider`) closes that gap:

**How it works:** after a login's password check succeeds (`isValid()` returns `true` from `super.isValid()`), it re-validates the *same* password against the realm's current password policy via `CustomPasswordPolicyManagerProvider`. If it no longer conforms, `UPDATE_PASSWORD` is added to the user's required actions (idempotently — it checks the user doesn't already have it), so they're prompted to set a new, compliant password right after this login instead of silently being allowed to keep an outdated one indefinitely.

`CustomPasswordCredentialProviderFactory` has to extend `PasswordCredentialProviderFactory` and duplicate several of its private constants (metrics/meter setup) to construct the custom provider — the class-level Javadoc flags this as something to re-check on every Keycloak upgrade, since it's coupled to Keycloak's internal implementation, not just its public API. `order()` is bumped by 10 over the built-in factory so this one takes precedence.

No configuration of its own — it uses whatever `PasswordPolicyProvider`s are registered for the realm's password policy, via the manager below.

---

## Custom Password Policy Manager (`custom`)

`CustomPasswordPolicyManagerProvider` replaces the default `PasswordPolicyManagerProvider` so individual policy checks can be selectively run, and so a **user-specific** password policy override is possible: if the user has a `passwordPolicy` attribute set, it's parsed and used instead of the realm's policy (via a `FakeRealm` — a `RealmModelDelegate` that overrides only `getPasswordPolicy()`/`setPasswordPolicy()` — temporarily swapped into the session context for the duration of validation).

`setPoliciesToSkip(List<String>)` lets a caller restrict which configured policy IDs are actually evaluated — used by `CustomPasswordCredentialProvider` above to only re-check `PASSWORD_HISTORY_ID` on the post-login conformance check.

> **Implementation note:** `getProviders()` filters the realm's configured policy IDs with `policiesToSkip::contains`, which *keeps* IDs present in that list rather than excluding them. In practice this means `policiesToSkip` acts as an allow-list, not a skip-list — with `CustomPasswordCredentialProvider`'s current call, only the password-history policy is re-checked after login, and all other policy rules are *not* re-enforced on that path despite the field's name. Worth confirming this matches your expectations before building further on it.

---

## SHA-1 Password Hash (`sha1`)

`SHA1HashProvider` implements `PasswordHashProvider` for the (weak, legacy) SHA-1 algorithm, using `DigestUtils.sha1Hex` — no salt, no iterations. Not meant for new passwords; it exists so credentials **imported** from a legacy system that hashed passwords with plain SHA-1 can still be verified on first login. Register the realm's users with `algorithm=sha1` in their imported `PasswordCredentialModel`, and once they log in successfully, point new password changes at a real (salted, iterated) hash provider.

---

## Have I Been Pwned Password Policy (`hibp`)

`HibpPasswordPolicyProvider` checks a candidate password against the [Have I Been Pwned](https://haveibeenpwned.com/Passwords) breach database (`api.pwnedpasswords.com`), using the k-anonymity range API — only the first 5 characters of the password's SHA-1 hash are sent, and the response's matching suffix lines are checked locally, so the full password/hash never leaves the server.

**Configuration:** a single integer, "maximum allowed breach occurrences" (`0` = reject the password if it's been breached at all). Configured per realm like any other built-in password policy.

**Failure handling:** HTTP requests use 1-second connect/read/connection-request timeouts; a non-200 response or `IOException` is logged and treated as "policy passed" — a HIBP outage never blocks login or password changes.

---

## Maximum Character Repetition Password Policy (`max-chars-rept`)

`MaximumCharacterRepetitionPasswordPolicyProvider` rejects passwords containing more than N consecutive repeated characters (e.g. `aaaa1234` with a max of `2` would be rejected for `aaaa`).

**Configuration:** the maximum allowed run length, default `2`.

---

## Testing

`SHA1HashProviderTest` and `MaximumCharacterRepetitionPasswordPolicyProviderTest` cover their respective classes' core logic directly, without needing a running Keycloak container.
