# Keycloak Extensions Demo

Demos, examples and playground for [Keycloak](https://www.keycloak.org) extensions, providers, SPI implementations, etc.

[![CI build](https://github.com/dasniko/keycloak-extensions-demo/actions/workflows/maven.yml/badge.svg)](https://github.com/dasniko/keycloak-extensions-demo/actions/workflows/maven.yml)
![](https://img.shields.io/github/license/dasniko/keycloak-extensions-demo?label=License)
![](https://img.shields.io/badge/Keycloak-26.0-blue)

>Provided _AS-IS_ - no warranties, no guarantees. Just for demonstration purposes only!

This repository contains the following extensions, and probably (most likely 😉) more...

## Keycloak User Storage Provider

[Flintstones](./flintstones-userprovider) - Demo user storage provider, providing some members of the Flintstones family, through an HTTP-base API and in writable mode, also possible to add new users.

## Keycloak Authenticators

[MagicLink Authenticator](./magiclink) - demo authenticator which sends a magic link to the user with which the user can login without needing to provide a password.

[Captcha Authenticator](./captcha) - demo authenticator in which the user needs to solve a math task and submit the result, before successful authentication.

[MFA Authenticator](./mfa-authenticator) - very simple(!!!) demo authenticator which prints a generated OTP to stdout.

[Conditional Authenticator](./conditional-authenticators) - conditions for authenticators which will decide upon
* a header and given value (or negated value) if `true`/`false`
* a authentication session note and given value (or negated value) if `true`/`false`

## Keycloak Event Listeners

### Session Restrictor

[Highlander](./event-listener) - demo event listener for Keycloak, allowing only the last session to survive (_Highlander mode - there must only be one!_), if a user logs in on multiple browsers/devices.
_(This was for long time not possible in Keycloak ootb, thus this event listener; since KC v19(?) this is natively supported.)_

### Event Forwarder

[AWS SNS Publisher](./event-listener) - demo event listener for Keycloak, simply forwarding/publishing all events to an AWS SNS topic.

### User Attribute Updater

[LastLoginTime](./event-listener) - demo event listener for Keycloak, storing the most recent login time in an user attribute.

## Custom Keycloak OIDC protocol token mapper

[LuckyNumberMapper](./tokenmapper) - example custom token mapper for Keycloak using the OIDC protocol.

## Keycloak REST endpoint/resource extension

[Custom Rest Resource](./rest-endpoint) - demo implementation for custom REST resources within Keycloak, public (unauthenticated) and secured (authenticated) endpoints.

## Custom Required Action

[MobileNumberRequiredAction](./requiredaction) - example which enforces the user to update its mobile phone number, if not already set.

## Custom Email Template & Sender Provider

[Email Provider](./email) for custom templates in JSON format (no actual emal, but for processing through external/3rd party services) and sending emails via a vendor specific (here: AWS SES) protocol, instead of SMTP.

## Demo Docker Compose Environment

There's a `docker-compose.yml` definition to use with Docker Compose. No Warranties, use at your own risk and fortune, I'm not giving any support to this!

Build and run all the stuff with:

    & ./mvnw clean package -DskipTests && docker compose up
