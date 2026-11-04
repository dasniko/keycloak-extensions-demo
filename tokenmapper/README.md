# Keycloak OIDC protocol token mapper

This is an example token mapper for Keycloak using the OIDC protocol.

You can configure the mapper with an upper and a lower bound and it will generate a random lucky number (integer) and maps it into the specified token(s) (and userinfo endpoint) to the specified claim.

![](./config.png)

## YouTube Video

[![](http://img.youtube.com/vi/5WBb176YqKg/maxresdefault.jpg)](http://www.youtube.com/watch?v=5WBb176YqKg "")

## Lookup User Roles mapper

`LookupUserRolesMapper` (`oidc-lookup-user-roles-mapper`) writes the realm and client roles of **another user**, found by
username, into the **access token** and the **token introspection response** (not ID token, not userinfo).

| Config | Key | Meaning |
|--------|-----|---------|
| Lookup Username | `lookup.username` | user whose roles are used |
| Merge with current user's roles | `merge` | `true` (default): add to the current user's roles; `false`: replace them (`realm_access`, `resource_access` and the claims below are cleared first) |
| Realm roles claim name | `realm.claim.name` | default `realm_access.roles`; nested claims are separated by dots |
| Realm role prefix | `realm.role.prefix` | prefix for each realm role |
| Client roles claim name | `client.claim.name` | default `resource_access.${client_id}.roles`; roles of all (enabled) clients |
| Client role prefix | `client.role.prefix` | prefix for each client role |

The keys are what you need when creating the mapper via the Admin REST API, realm import or Terraform. Additionally, the
usual `access.token.claim` and `introspection.token.claim` flags select the targets.

`${client_id}` is replaced by the client ID owning the roles. It works in the client roles claim name and in the client role
prefix, e.g. claim name `my.clients.${client_id}` with prefix `${client_id}:` yields `{"my": {"clients": {"my-app": ["my-app:admin"]}}}`.
In the claim name, dots in the client ID are escaped, so a client `my.app` stays one path component.

Roles are resolved like the built-in role mappers do: composites expanded, restricted by the requesting client's scope
(full scope allowed / role scope mappings). If the user is not found or is disabled, the token stays unchanged and a warning is logged.

Things to be aware of:

- The mapper has priority 41 (role mappers: 40, script mapper: 50) so it runs after the built-in role mappers.
- The audience resolve mapper (priority 30) derives `aud` from the **current user's** roles, before this mapper runs. If that
  mapper is active for the token being built, this mapper adjusts `aud` to the looked-up user's client roles: it adds them
  (merge) or replaces the current user's role-based audiences (replace). An audience for the same client that comes from an
  explicit audience mapper is removed in replace mode too, as the two cannot be told apart.
- In replace mode, roles written by other mappers to claim paths *other* than `realm_access`, `resource_access` and the two
  configured paths are not removed.
- Only realm admins can configure mappers, but this effectively lets a client's tokens carry another user's roles. Use a
  dedicated, minimally privileged user.
