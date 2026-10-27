# Keycloak Workflows

Custom steps for Keycloak's [workflows](https://www.keycloak.org/docs/latest/server_admin/#_workflows) feature (feature flag `workflows`).

## Step `disable-expired-user`

Disables a user if the date(time) stored in a user attribute lies in the past. Users that are already disabled, have no value in the attribute, or
have a value that is not an ISO date(time) are left untouched (the latter is logged as a warning).

| `with` setting | Description |
|---|---|
| `attribute` | Name of the user attribute holding the expiry date(time). Required. |

Accepted values (ISO-8601): `2025-01-31`, `2025-01-31T10:15`, `2025-01-31T10:15:30`, `2025-01-31T10:15:30+02:00`, `2025-01-31T10:15:30Z`.
A value without an offset (date only or local date-time) is interpreted in the server's time zone; a plain date means the start of that day.

```yaml
name: disable-expired-users
on: ...
steps:
  - uses: disable-expired-user
    with:
      attribute: accountExpiry
```
