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
  { "name": "yearOfBirth", "field": "yearOfBirth", "type": "integer" },
  { "name": "addressJson", "field": "address", "type": "json", "readOnly": true, "group": "user-metadata" },
  { "name": "city", "field": "address", "property": "city", "group": "user-metadata" }
]
```

### Complex values

`type: "json"` exposes a whole object (or array) as its serialized JSON string. Useful when the consumer just wants the payload
through; usually paired with `readOnly`, since hand-editing raw JSON in a text field is a footgun — but it does round-trip if you
leave it writable.

`property` maps a single member out of a complex object. **Writing replaces the object with one built from that member alone**, so
`{"street": "…", "city": "Bedrock"}` becomes `{"city": "Rock Vegas"}`. That only happens when the mapped member actually changes —
`AttributeMapping#changes` compares in Keycloak attribute space, so an unrelated update never truncates the record. `property`
combines with `multivalued` (project a member out of each element of an array) and with `type` (including `json`).

### Attribute groups

`group` places the attribute in a user profile attribute group. The group must be **declared in the realm's user profile
configuration** (Realm settings → User profile) — Keycloak builds the group list of the profile payload purely from that config,
and an attribute pointing at an undeclared group falls into no group bucket and is **not rendered at all**. `user-metadata` ships
in the default configuration, so it works out of the box.

Two things guard against that trap: the configuration is rejected on save if it names a group the realm does not declare, and if
the group disappears from the realm afterwards the attribute is shown ungrouped (with a warning) rather than vanishing.

Only single-level members are supported. A dotted path (`department.manager.id`) would be a small extension on read and write;
see the note in the repository `CLAUDE.md`.

That is the configuration the integration tests use. The demo records carry `pictureUrl` (string), `phoneNumbers` (JSON array)
`yearOfBirth` (JSON number) and `address` (JSON object), so all the shapes are exercised end to end — including that a value written back keeps its JSON
type rather than becoming the string Keycloak stores internally. Several mappings may read the same field, but only one of them
may write it.

| Key | Required | Default | Meaning |
|-----|----------|---------|---------|
| `name` | yes | — | attribute name on the Keycloak side |
| `field` | yes | — | field name in the external user record |
| `type` | no | `string` | `string`, `integer`, `long`, `boolean` or `json` — the JSON type of the external value |
| `multivalued` | no | `false` | whether the attribute can hold more than one value |
| `readOnly` | no | `false` | if true, the attribute is never written back to the external source |
| `delimiter` | no | — | store a multivalued attribute externally as a single delimited string instead of a JSON array |
| `property` | no | — | the external value is a complex object; map only this member of it |
| `group` | no | — | user profile attribute group to show the attribute in; must be declared in the realm |

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
