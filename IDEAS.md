# IDEAS for SPI implementations

## Group/Role Synchronization for ext. IdPs

Use the content of a specific claim (configurable) in the user id_token and/or userinfo endpoint (use also the access_token, if 'access_token is JWT' setting in IdP config) to create groups or roles (configurable what to create) from automatically.
Then, also assign the current authenticating user to these either groups or roles.

As roles can be realm roles or client roles, the implementation should be able to handle both (configurable, if client roles, select the client in mapper config, see also existing LDAP role mapper).

If the configured claim is not present, or the claim value is empty, remove the user from synced groups/roles.
The values of the configured claim may be a single string or an array of strings. Should be automatically detected.
