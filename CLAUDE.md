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

### Attribute mapping in `flintstones-userprovider`

Attributes beyond the core `UserModel` fields are configured, not coded. Decided against a sub-component mapper SPI (the
LDAP `ldap-mapper` pattern): the new admin console renders custom user federation providers with `CustomProviderSettings`,
which has no mappers tab, so those mappers would only be manageable via the components REST API. `UiPageProvider` would give
a list UI but is gated behind the experimental `declarative-ui` feature.

Instead: a single `attributeMappings` config property (`TEXT_TYPE`) holding a JSON array of mapping definitions, plus
`@JsonAnySetter`/`@JsonAnyGetter` on `FlintstoneUser` so the DTO is agnostic to the external schema. See the module README for
the format. Key pieces:

- `AttributeMappings` — parse/validate/cache; the parsed result is cached in `ComponentModel.getNote()`, keyed on the raw
  config string so a config update invalidates it.
- `AttributeMapping#toAttributeValues` / `#toExternalValue` — the two conversion directions.
- `FlintstonesUserStorageProvider implements UserProfileDecorator` — declares mapped attributes on the user profile, otherwise
  they are unmanaged attributes and invisible without a realm `unmanagedAttributePolicy`.
- Jackson will not honour `@JsonCreator` on the `ValueType` enum here, so `AttributeMapping.create` takes the type as a `String`
  and resolves it via `ValueType.from`. Pinned by `AttributeMappingsTest.rejectsUnknownType`.
- Complex external values are handled declaratively, not by a scripting hook. A JS transform (Nashorn/GraalJS) was considered and
  rejected: admin-editable script = code execution in the auth server, which is why Keycloak's own `SCRIPTS` feature is still
  `Type.PREVIEW`; plus Nashorn left the JDK in 15. `type: "json"` and `property` cover the cases we have. If an arbitrary
  transform is ever needed, add a *named* transform registered in Java and referenced from the config — never inline script.
- `property` supports a single level only. A dotted path (`department.manager.id`) is a deliberate TODO: read would walk the maps,
  write would rebuild the nested minimal objects. Left out because nothing needs it yet, and a dotted path is ambiguous when a
  key legitimately contains a dot.
- `property` writes use **replace** semantics: the object is rebuilt from the mapped member alone. Guarded by
  `AttributeMapping#changes`, which compares in Keycloak attribute space so an unrelated update cannot truncate the record.

Note when testing: Keycloak's user profile filters read-only attributes and never pushes unchanged attributes down to the
provider, so the adapter's `readOnly` guard and `AttributeMapping#changes` are *not* reachable from the container tests. Both are
defence-in-depth for callers that bypass the user profile; `changes` is covered by unit tests instead.

### Key Conventions

- Java 21, tabs indentation, 140-char line limit (see `.editorconfig`)
- Lombok (`@Slf4j`, `@RequiredArgsConstructor`, etc.) used throughout
- Always use the latest Keycloak API — check `https://api.github.com/repos/keycloak/keycloak/releases/latest` for current version
- Fat JAR artifact naming: `${project.groupId}-${project.artifactId}.jar`
