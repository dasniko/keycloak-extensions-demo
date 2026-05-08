# Keycloak Identity Provider Extensions

Extensions for Keycloak's Identity Provider (IdP) broker functionality.

---

## Custom OIDC Identity Provider

A custom OIDC identity provider that extends Keycloak's built-in OIDC provider with an additional configuration option: sending the `logout_hint` parameter (populated from the `login_hint` claim in the id_token) in browser-initiated logout requests.

---

## Claim to Group/Role Sync Mapper (`ClaimToGroupRoleSyncMapper`)

An `IdentityProviderMapper` that reads a configurable claim from an external IdP's token and **fully synchronizes** the authenticating user's group memberships or role assignments based on its values.

### How it works

When a user authenticates via an external IdP (OIDC or Keycloak-OIDC), Keycloak receives an id_token, an access token, and optionally a userinfo response. This mapper reads a named claim from those tokens and uses its value(s) to manage the user's groups or roles inside Keycloak.

The claim value is **auto-detected** as either a plain string or a JSON array of strings — no configuration needed for the format.

**Full sync** is applied on every login: groups/roles present in the claim are assigned (and auto-created in Keycloak if they don't exist yet), and any previously assigned groups/roles that are no longer present in the claim are removed.

If the claim is absent from the token entirely, the mapper skips the sync and leaves existing assignments untouched. An empty array in the claim is treated as an explicit "user has no memberships", and all previously assigned groups/roles managed by this mapper are removed.

### Sync types

The mapper supports three sync targets, configurable per mapper instance:

| Sync Type | Behavior |
|-----------|----------|
| `GROUPS` | Manages the user's **top-level group** memberships. Groups are created at the realm's top level. Subgroups are never touched. |
| `REALM_ROLES` | Manages the user's **realm role** assignments. Only directly assigned realm roles are considered; composite or client roles are not affected. |
| `CLIENT_ROLES` | Manages the user's role assignments for a **specific client**. The client is selected by Client ID in the mapper config. Only roles belonging to that client are touched. |

### Auto-creation

If a group or role named after a claim value does not yet exist in Keycloak, it is created automatically:
- Groups are created as top-level groups in the realm.
- Realm roles are created as plain realm roles.
- Client roles are created on the configured client.

### Configuration

| Property | Required | Description |
|----------|----------|-------------|
| **Claim** | yes | The claim name to read from the token. Supports nested claims using dot-notation, e.g. `roles` or `resource_access.myclient.roles`. |
| **Sync Type** | yes | One of `GROUPS`, `REALM_ROLES`, `CLIENT_ROLES`. Defaults to `GROUPS`. |
| **Client ID** | only for `CLIENT_ROLES` | The `clientId` of the client whose roles are managed. |

### Implementation notes

The mapper extends Keycloak's `AbstractClaimMapper`, which searches the validated access token, the id_token, and the userinfo endpoint response in that order when resolving the configured claim. This makes the mapper work regardless of which token the IdP places the claim in.

The removal scope is intentionally precise to avoid side effects:
- For `GROUPS`: only top-level groups (no parent) are removed; nested group memberships are never touched.
- For `REALM_ROLES`: only directly assigned realm roles (`getRealmRoleMappingsStream`) are considered.
- For `CLIENT_ROLES`: only roles of the configured client (`getClientRoleMappingsStream(client)`) are considered.

All three `IdentityProviderMapper` lifecycle methods (`importNewUser`, `updateBrokeredUser`, `updateBrokeredUserLegacy`) invoke the same sync logic, so full sync runs regardless of which `IdentityProviderSyncMode` is configured on the IdP.

The mapper is compatible with `oidc` and `keycloak-oidc` identity provider types.
