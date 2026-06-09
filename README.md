# Spring Boot Auto Port

A Spring Boot starter that automatically assigns an available port when the configured port is already in use.

Instead of crashing with `Address already in use`, your application scans for the next free port and starts on it — zero code changes required.

## Installation

```xml
<dependency>
    <groupId>io.github.ahmed-bulbul</groupId>
    <artifactId>spring-boot-auto-port-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## How it works

At startup, before the server binds, the library checks whether `server.port` is available:

- If the port is **free** — startup proceeds normally.
- If the port is **occupied** — it scans upward from that port until it finds a free one, then sets `server.port` to that value.

## Configuration

All properties are optional.

| Property           | Default | Description                                              |
|--------------------|---------|----------------------------------------------------------|
| `auto.port.enabled` | `true`  | Set to `false` to disable auto port selection            |
| `auto.port.start`   | `8080`  | Base port to use when `server.port` is not set           |
| `auto.port.max`     | `9000`  | Upper bound of the scan range (inclusive)                |

### Example

```properties
# application.properties
auto.port.start=8080
auto.port.max=8090
```

If port `8080` is taken, the app will try `8081`, `8082`, … up to `8090`. If none are free, startup fails with a clear error message.

## Behaviour details

- `server.port=0` (Spring's random-port mode) is respected and left untouched.
- If `server.port` is explicitly set and that port is free, it is kept as-is.
- The port resolution runs last among all `EnvironmentPostProcessor`s, so it sees the final value of `server.port` after all other configuration sources.

## Requirements

- Java 17+
- Spring Boot 3.x

## License

Apache License 2.0
