# Keycloak Extensions Demo

Demos and playground for [Keycloak](https://www.keycloak.org) extensions, providers, SPI implementations, etc.

> This repository is currently under construction and will be completed step-by-step.
> In the meantime, be patient and surf through my other [keycloak repositories](https://github.com/dasniko?tab=repositories&q=keycloak) here.

## Keycloak User Storage Provider

[Flintstones](./user-provider) - demo user storage provider, providing some members of the flintstones families in a read-only mode.

## Keycloak Session Restrictor Event Listener

[SessionRestrictor](./session-restrictor) - demo event listener for Keycloak, allowing only the last session to survive, if a user logs in on multiple browsers/devices.

## Custom Keycloak OIDC protocol token mapper

[LuckyNumberMapper](./tokenmapper) - example custom token mapper for Keycloak using the OIDC protocol.
