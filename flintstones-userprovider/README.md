# Flintstones User Provider (Keycloak User Storage Provider) Example

Demo user storage provider, providing some members of the Flintstones family in a read/write mode, from an (in-memory) http service repository.

## Attribute mappings

Attributes beyond the core `UserModel` fields (username, email, first/last name) are **not** hardcoded. The external user record
(`FlintstoneUser`) captures every unknown JSON field via `@JsonAnySetter`/`@JsonAnyGetter`, and the provider's `attributeMappings`
configuration decides which of those fields become Keycloak user attributes.

The config property is a JSON array, edited as a textarea in the admin console (User federation → the-flintstones → Attribute
mappings):

```json
[
  { "name": "picture", "field": "pictureUrl" },
  { "name": "avatar", "field": "pictureUrl", "readOnly": true },
  { "name": "phone", "field": "phoneNumbers", "multivalued": true },
  { "name": "yearOfBirth", "field": "yearOfBirth", "type": "integer" }
]
```

That is the configuration the integration tests use. The demo records carry `pictureUrl` (string), `phoneNumbers` (JSON array)
and `yearOfBirth` (JSON number), so all three shapes are exercised end to end — including that a value written back keeps its JSON
type rather than becoming the string Keycloak stores internally. Several mappings may read the same field, but only one of them
may write it.

| Key | Required | Default | Meaning |
|-----|----------|---------|---------|
| `name` | yes | — | attribute name on the Keycloak side |
| `field` | yes | — | field name in the external user record |
| `type` | no | `string` | `string`, `integer`, `long` or `boolean` — the JSON type of the external value |
| `multivalued` | no | `false` | whether the attribute can hold more than one value |
| `readOnly` | no | `false` | if true, the attribute is never written back to the external source |
| `delimiter` | no | — | store a multivalued attribute externally as a single delimited string instead of a JSON array |

Notes:

- The configuration is validated on save (`validateConfiguration`), so a typo surfaces as an error in the admin console rather
  than as a silent runtime no-op. Root attributes (`username`, `email`, `firstName`, `lastName`, `locale`) are rejected — those
  are first-class `UserModel` fields handled by the adapter itself.
- Mapped attributes are declared on the user profile via `UserProfileDecorator#decorateUserProfile`. Without that they would be
  *unmanaged* attributes and therefore invisible unless the realm sets an `unmanagedAttributePolicy`.
- Values the external source cannot deliver in the configured shape are skipped with a warning (a bad value must not break a
  login); values that cannot be written are rejected with a `ModelException`.
- `readOnly` is enforced by the write condition `decorateUserProfile` puts on the attribute, so Keycloak drops the change before
  it reaches the adapter. The adapter checks the flag again for callers that bypass the user profile.
- Because unknown fields are captured rather than dropped, a read-modify-write of a user preserves everything the API sent, even
  fields this extension knows nothing about.
