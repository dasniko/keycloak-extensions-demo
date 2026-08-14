# Keycloak Scheduled Task

Defines a private, internal SPI (`scheduled-task`, `isInternal() == true`) for fixed-interval background jobs, built on top of Keycloak's own `TimerProvider`, with the cluster-safety boilerplate already handled — implementations only need to say *what* to run and *how often*.

> Cron expressions aren't supported here — for that, see the `initializer` module's `timer` provider instead, which layers `cron-utils` on top of `TimerProvider`. This module is for the simpler "every N seconds" case.

---

## How it works

`ScheduledTaskProviderFactory` (abstract) does the actual scheduling:

- `init(Config.Scope)` reads the `intervalSeconds` config property. A value `<= 0` (the default, `-1`) means the task is **disabled**; nothing gets scheduled.
- `postInit()` registers a listener for Keycloak's `PostMigrationEvent` (fired once startup/DB migration is complete) and, when it fires, calls `TimerProvider.scheduleTask(...)` with the configured interval (converted to milliseconds) and the provider's own ID.
- `close()` cancels the task via `TimerProvider.cancelTask(getId())` on shutdown.

A concrete provider only needs to implement `ScheduledTaskProvider.getScheduledTask()`, returning a `ScheduledTask` (i.e. a `session -> { ... }` lambda) — the interval and lifecycle wiring above is shared.

---

## Example Scheduled Task (`example`)

`ExampleScheduledTaskProvider` is the demo implementation. Its task body doesn't just log — it demonstrates the standard Keycloak pattern for making a periodic task **cluster-safe**: in a multi-node deployment, `TimerProvider` runs independently *on every node*, so without extra coordination the same task would fire once per node on every interval. `ClusterProvider.executeIfNotExecuted(taskKey, timeToWaitMs, callable)` uses Keycloak's cluster/cache coordination so that only one node actually runs the callable within the given time window (10 seconds here); the others skip it.

**Configuration:**

| Property | Env var | Default | Description |
|---|---|---|---|
| `intervalSeconds` | `KC_SPI_SCHEDULED_TASK_EXAMPLE_INTERVAL_SECONDS` | `-1` (disabled) | How often the task runs, in seconds. |

Since the SPI is internal, this isn't configurable from the admin console — only via `keycloak.conf` / environment variables.
