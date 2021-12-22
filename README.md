# Keycloak Extensions Demo

Demos and playground for [Keycloak](https://www.keycloak.org) extensions, providers, SPI implementations, etc.

> This repository is currently under construction and will be completed step-by-step.
> In the meantime, be patient and surf through my other [keycloak repositories](https://github.com/dasniko?tab=repositories&q=keycloak) here.

## Keycloak User Storage Provider

[PeanutsUserProvider](./user-provider) - demo user storage provider, providing some members of the peanuts in a read-only mode, via an external API.

## Keycloak Session Restrictor Event Listener

[Highlander](./session-restrictor) - demo event listener for Keycloak, allowing only the last session to survive (_Highlander mode - there must only be one!_), if a user logs in on multiple browsers/devices.

## Custom Keycloak OIDC protocol token mapper

[LuckyNumberMapper](./tokenmapper) - example custom token mapper for Keycloak using the OIDC protocol.

## Keycloak REST endpoint/resource extension

[RestExample](./rest-endpoint) - demo implementation for custom REST resources within Keycloak, public (unauthenticated) and secured (authenticated) endpoints.

## Custom Required Action

[MobileNumberRequiredAction](./requiredaction) - example which enforces the user to update its mobile phone number, if not already set.

## Custom Action Token

[ActionToken](./actiontoken) _t.b.d._
