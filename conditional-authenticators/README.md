# Keycloak Conditional Authenticators

Custom `ConditionalAuthenticator` implementations, used as the condition step of a Keycloak **Conditional Subflow** to decide whether the rest of that subflow runs at all — e.g. "only require MFA if the request comes from outside the office network."

All four share a common base, `AbstractConditionalAuthenticator`, which supplies the boilerplate every conditional authenticator needs: `getSingleton()`, `REQUIREMENT_CHOICES` (`REQUIRED`/`DISABLED` only — conditionals aren't themselves conditional or alternative), `requiresUser() == false`, and a `Negate output` config property shared by every subclass (`isNegateOutput()`) that flips the match result with a boolean XOR.

---

## Condition - Authentication Method Reference (`conditional-amr`)

`ConditionalAmrAuthenticator` checks whether a specific authentication method (`amr` value, e.g. `otp`, `pwd`, `sms`) has already been used earlier in the *current* authentication session, by parsing the session's completed-executions note (`Constants.AUTHENTICATORS_COMPLETED`) via `AmrUtils`.

**Configuration:**

| Property | Description |
|---|---|
| AMR value | The AMR value that must be present among the already-completed executions. |
| Negate output | If set, matches when the value is **absent** instead. |

---

## Condition - Authentication Session Note (`conditional-auth-note`)

`ConditionalAuthNoteAuthenticator` checks whether a given auth-session note currently equals an expected value. Useful for branching a flow based on state set by an earlier custom authenticator (e.g. a note set during a broker login, or during a previous step in the same flow).

**Configuration:**

| Property | Description |
|---|---|
| AuthNote name | Name of the auth-session note to read. |
| AuthNote value | The value it must equal for the condition to match. |
| Negate output | If set, matches when the note does **not** equal that value. |

---

## Condition - CIDR (`conditional-cidr`)

`ConditionalCidrAuthenticator` checks whether the client's remote IP address falls within one or more allowed CIDR ranges (comma-separated; a bare IP is treated as a `/32` or `/128`). Works for both IPv4 and IPv6.

**Configuration:**

| Property | Description | Default |
|---|---|---|
| CIDRs | Comma-separated list of allowed CIDR ranges, e.g. `10.0.0.0/8, 192.168.1.0/24`. | `0.0.0.0/0` (matches everything) |
| Negate output | If set, matches when the IP is **outside** all given ranges — e.g. to require extra verification only for connections from *outside* a trusted network. |

---

## Condition - Custom Header (`conditional-custom-header`)

`ConditionalHeaderAuthenticator` checks whether a given HTTP request header equals an expected value (case-insensitive) — e.g. to branch a flow based on a header set by a trusted reverse proxy or API gateway in front of Keycloak.

**Configuration:**

| Property | Description | Default |
|---|---|---|
| Header name | Name of the HTTP header to read. | `X-Custom-Header` |
| Expected header value | Value it must equal for the condition to match. | `my-custom-value` |
| Negate output | If set, matches when the header does **not** equal that value. |

---

## Testing

`IsIpInSubnetTest` covers `ConditionalCidrAuthenticator#isIpInSubnet` in isolation (no Testcontainers needed), across IPv4/IPv6 addresses and edge cases like bare IPs without a prefix.
