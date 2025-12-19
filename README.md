# Keycloak Extensions Demo

Demos, examples and playground for [Keycloak](https://www.keycloak.org) extensions, providers, SPI implementations, etc.

[![CI build](https://github.com/dasniko/keycloak-extensions-demo/actions/workflows/maven.yml/badge.svg)](https://github.com/dasniko/keycloak-extensions-demo/actions/workflows/maven.yml)
![GitHub Last Commit](https://img.shields.io/github/last-commit/dasniko/keycloak-extensions-demo)
![License](https://img.shields.io/github/license/dasniko/keycloak-extensions-demo?label=License)  
[![Keycloak Version](https://img.shields.io/badge/Keycloak-999.0.0&dash;SNAPSHOT-blue)](https://www.keycloak.org)
![Java Version](https://img.shields.io/badge/Java-21-f89820)
[![GitHub Stars](https://img.shields.io/github/stars/dasniko/keycloak-extensions-demo)](https://github.com/dasniko/keycloak-extensions-demo/stargazers)
[![GitHub Forks](https://img.shields.io/github/forks/dasniko/keycloak-extensions-demo)](https://github.com/dasniko/keycloak-extensions-demo/forks)

>Provided _AS-IS_ - no warranties, no guarantees.  
>Just for demonstration purposes only!

This repository contains the following extensions, and probably (most likely 😉) more...

## Extension Modules

### Authentication & Authorization

- **[MagicLink Authenticator](./magiclink)** - Passwordless authentication via magic link sent to user's email
- **[Authenticators](./authenticators)** - Collection of various demo authenticators including:
  - CAPTCHA integration
  - Deny/block authenticator
  - MFA implementations
  - MFA enrollment flows
  - Redirect authenticators
  - Registration enhancements
- **[Conditional Authenticators](./conditional-authenticators)** - Conditional logic for authentication flows based on:
  - HTTP headers and values (or negated values)
  - Authentication session notes and values
  - Custom conditions

### User Management

- **[Flintstones User Provider](./flintstones-userprovider)** - Demo user storage provider with HTTP-based API, supporting read/write operations with the Flintstones family members
- **[Required Actions](./requiredaction)** - Custom required actions (e.g., MobileNumberRequiredAction for enforcing mobile phone number updates)
- **[Validators](./validators)** - Custom validation providers for user attributes and forms

### Event Processing

- **[Event Listeners](./event-listener)** - Multiple event listener implementations:
  - **Highlander** - Session restrictor allowing only the last session to survive (single session per user)
  - **AWS SNS Publisher** - Forwards all Keycloak events to AWS SNS topics
  - **LastLoginTime** - Stores most recent login timestamp in user attributes

### Protocol & Tokens

- **[Token Mappers](./tokenmapper)** - Custom OIDC protocol token mappers (e.g., LuckyNumberMapper)
- **[Action Tokens](./actiontoken)** - Custom action token implementations for special-purpose links

### APIs & Integrations

- **[REST Endpoint](./rest-endpoint)** - Custom REST resources within Keycloak with both public and authenticated endpoints
- **[Custom SMS SPI](./custom-sms-spi)** - Custom SMS provider service provider interface

### Administration & Operations

- **[Admin UI Extensions](./admin-ui)** - Custom Admin Console UI components and extensions
- **[Initializer](./initializer)** - Realm initialization and configuration automation, including scheduled tasks
- **[Scheduled Tasks](./scheduled-task)** - Background task scheduling and cron-based operations

### Communication

- **[Email Provider](./email)** - Custom email templates and senders:
  - JSON format templates for external service processing
  - AWS SES integration instead of SMTP
  - Vendor-specific email protocols

### Security & Policies

- **[Passwords](./passwords)** - Password policy extensions and custom password validators

### Utilities

- **[Utils](./utils)** - Shared utilities and helper classes used across extensions
- **[Various](./various)** - Miscellaneous extension examples and experimental features

## Demo Docker Compose Environment

A `docker-compose.yml` is provided for local development and testing with Keycloak. The setup includes:

- **Keycloak** running in development mode with preview features enabled
- **Remote debugging** available on port 8787
- **All extension modules** automatically mounted into Keycloak's providers directory
- **Persistent data** volume for Keycloak data

### Quick Start

Build all extensions and start Keycloak:

```bash
./mvnw clean package -DskipTests && docker compose up
```

### Access

- **Keycloak Admin Console**: http://localhost:8080
- **Admin Credentials**: username `admin`, password `admin`
- **Remote Debug Port**: 8787

### Included Extensions

All 18 extension modules are automatically loaded:
- actiontoken, admin-ui, authenticators, conditional-authenticators
- custom-sms-spi, email, event-listener, flintstones-userprovider
- initializer, magiclink, passwords, requiredaction
- rest-endpoint, scheduled-task, tokenmapper, utils
- validators, various

> **Note**: No warranties or support provided - use at your own risk for demonstration purposes only!
