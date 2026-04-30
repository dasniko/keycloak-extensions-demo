# IDEAS for SPI implementations

## Group/Role Synchronization for ext. IdPs

Use the content of a specific claim (configurable) in the user id_token and/or userinfo endpoint (use also the access_token, if access_token is JWT) to create groups or roles (configurable what to create) from.
Then, also assign the current authenticating user to these either groups or roles.

As roles can be realm roles or client roles, the implementation should be able to handle both (configurable, see also existing LDAP role mapper).
