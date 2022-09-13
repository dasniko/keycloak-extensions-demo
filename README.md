# Keycloak Extensions Demo

Demos, examples and playground for [Keycloak](https://www.keycloak.org) extensions, providers, SPI implementations, etc.

[![CI build](https://github.com/dasniko/keycloak-extensions-demo/actions/workflows/maven.yml/badge.svg)](https://github.com/dasniko/keycloak-extensions-demo/actions/workflows/maven.yml)
![](https://img.shields.io/github/license/dasniko/keycloak-extensions-demo?label=License)
![](https://img.shields.io/badge/Keycloak-18.0.0-blue)

## Keycloak User Storage Provider

[PeanutsUserProvider](./user-provider) - demo user storage provider, providing some members of the peanuts in a read-only mode, via an external API.

## Keycloak Authenticators

[MagicLink Authenticator](./magiclink) - demo authenticator which sends a magic link to the user with which the user can login without needing to provide a password.

[Captcha Authenticator](./captcha) - demo authenticator in which the user needs to solve a math task and submit the result, before successful authentication.

## Keycloak Event Listeners

### Session Restrictor

[Highlander](./event-listener) - demo event listener for Keycloak, allowing only the last session to survive (_Highlander mode - there must only be one!_), if a user logs in on multiple browsers/devices.

### Event Forwarder

[AWS SNS Publisher](./event-listener) - demo event listener for Keycloak, simply forwarding/publishing all events to an AWS SNS topic.

### User Attribute Updater

[LastLoginTime](./event-listener) - demo event listener for Keycloak, storing the most recent login time in an user attribute.

## Custom Keycloak OIDC protocol token mapper

[LuckyNumberMapper](./tokenmapper) - example custom token mapper for Keycloak using the OIDC protocol.

## Keycloak REST endpoint/resource extension

[RestExample](./rest-endpoint) - demo implementation for custom REST resources within Keycloak, public (unauthenticated) and secured (authenticated) endpoints.

## Custom Required Action

[MobileNumberRequiredAction](./requiredaction) - example which enforces the user to update its mobile phone number, if not already set.

## Custom Action Token

[ActionToken](./actiontoken) _t.b.d._
