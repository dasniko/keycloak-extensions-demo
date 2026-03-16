# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules (skip tests)
./mvnw clean package -DskipTests

# Build and run all tests
./mvnw clean install

# Build with parallelism (as used in CI)
./mvnw -B -U -T 1C clean install

# Run tests for a single module
./mvnw test -pl event-listener

# Run a single test class
./mvnw test -pl event-listener -Dtest=LastLoginTimeListenerTest

# Start local Keycloak with all extensions mounted
./mvnw clean package -DskipTests && docker compose up
```

Keycloak is available at `http://localhost:8080` (admin/admin) after `docker compose up`.
Remote debug port: `8787`.

## Architecture

This is a Maven multi-module project. Each module is an independent Keycloak extension (SPI implementation) packaged as a fat JAR via `maven-shade-plugin` and deployed to `/opt/keycloak/providers/`.

### SPI Registration Pattern

All providers use `@AutoService` (Google Auto Service) to generate `META-INF/services` entries automatically — no manual service file editing needed.

```java
@AutoService(EventListenerProviderFactory.class)
public class MyListenerFactory implements EventListenerProviderFactory {
    public static final String PROVIDER_ID = "my-listener";
    // implement create(), init(), postInit(), close(), getId()
}
```

### Module Overview

| Module | SPI Type |
|--------|----------|
| `event-listener` | `EventListenerProvider` |
| `authenticators` | `Authenticator` |
| `conditional-authenticators` | `ConditionalAuthenticator` |
| `requiredaction` | `RequiredActionProvider` |
| `flintstones-userprovider` | `UserStorageProvider` |
| `rest-endpoint` | `RealmResourceProvider` |
| `tokenmapper` | `ProtocolMapper` |
| `magiclink` | `Authenticator` (passwordless) |
| `actiontoken` | `ActionTokenHandler` |
| `email` | `EmailTemplateProvider` |
| `passwords` | `PasswordPolicyProvider` |
| `validators` | `ClientValidationProvider` |
| `scheduled-task` | `TimerProvider` |
| `initializer` | `RealmResourceProvider` (init-on-startup) |
| `utils` | Shared test base classes only |

### Testing

Tests use **Testcontainers** (`testcontainers-keycloak`) to spin up a real Keycloak instance. The extension JAR built by the module is mounted into the container before tests run.

Extend `TestBase` (from the `utils` module, test scope) for common helpers:
- `initTestRealm()` — create a test realm
- `requestToken()` — OAuth2 token requests
- `getUser()` / `updateUser()` — admin API helpers
- `parseToken()` — JWT decode

Realm configuration for tests lives in `src/test/resources/` as JSON import files.

### Key Conventions

- Java 21, tabs indentation, 140-char line limit (see `.editorconfig`)
- Lombok (`@Slf4j`, `@RequiredArgsConstructor`, etc.) used throughout
- Always use the latest Keycloak API — check `https://api.github.com/repos/keycloak/keycloak/releases/latest` for current version
- Fat JAR artifact naming: `${project.groupId}-${project.artifactId}.jar`
