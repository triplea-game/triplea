# lib/feign-common

Shared infrastructure for building typed Feign HTTP clients with consistent
JSON serialization, retry behavior, and timeout defaults. All HTTP clients in
the project (lobby-client, game-headed, websocket-server) depend on this
module.

## Key Classes

### HttpClient\<T\>

Central factory — wraps `Feign.builder()` with project-wide defaults:

- **Encoding/Decoding**: Gson-based `JsonEncoder` and `JsonDecoder` (see below).
- **Timeouts**: 5 s connect, 20 s read (`DEFAULT_CONNECT_TIMEOUT_MS`,
  `DEFAULT_READ_TIME_OUT_MS`).
- **Retry**: up to 3 attempts with 100 ms initial interval, 1 s max
  (`Retryer.Default`).
- **Headers**: every request gets `Content-Type: application/json` and
  `Accept: application/json`. Callers can pass additional headers via the
  `Map<String, String>` constructor parameter.
- **Logging**: `Logger.Level.BASIC` at TRACE level. `sendKeepAlive` requests
  are silently suppressed to reduce noise.
- **Error decoder**: delegates to `FeignException.errorStatus` (standard Feign
  behavior).

Usage pattern — Feign interface classes provide a static `newClient` that
delegates here:

```java
T client = HttpClient.newClient(MyFeignInterface.class, serverUri);
T client = HttpClient.newClient(MyFeignInterface.class, serverUri, extraHeaders);
```

`HttpClient` also implements `Supplier<T>` so it can be used as a lazy
provider.

### JsonEncoder

Custom `Encoder` that passes `String` bodies through verbatim (avoiding
Gson's default behavior of wrapping strings in extra quotes). All other types
delegate to `GsonEncoder`.

### JsonDecoder

Custom Gson `Decoder` with a registered `Instant` deserializer. The server
represents timestamps as `epochSecond.epochNano` (e.g., `1559794806.329342000`).
The decoder splits on `.` and reconstructs via `Instant.ofEpochSecond(sec, nano)`.
A missing `.` causes a `Preconditions.checkState` failure.

### GenericServerResponse

Simple success/failure DTO (`boolean success`, `@Nullable String message`).
Has a static `SUCCESS` singleton for the common case.

### HttpConstants / HttpClientConstants

- `HttpConstants` — header-line constants (`ACCEPT_JSON`, `CONTENT_TYPE_JSON`)
  used in Feign `@Headers` annotations.
- `HttpClientConstants` — length limits for GitHub issue titles (125) and
  bodies (65 536).

## Conventions

- **Gson everywhere** — both encoder and decoder use Gson, not Jackson. The
  custom `Instant` format (`epoch.nano`) must be matched by the server side.
- **No per-client customization of timeouts or retry** — all clients share the
  same defaults. To change behavior, modify `HttpClient` (affects everything).
- **Feign interfaces live outside this module** — this module only provides the
  builder; the annotated interfaces are in `http-clients/` and `game-app/`.

## Gotchas

- `JsonDecoder` requires the `epochSecond.epochNano` timestamp format with a
  literal `.` separator. Any other `Instant` representation (ISO-8601, epoch
  millis) will throw an `IllegalStateException`.
- `JsonEncoder` special-cases `String.class` by exact type name match — a
  subtype of `String` (hypothetically) would still go through Gson.
- The `gsonDecoder` field in `HttpClient` is a shared static instance. The
  `Gson` object it wraps is thread-safe, but be aware of the singleton
  lifecycle.

## Testing

JUnit 5 with Hamcrest matchers. Tests cover:

- `JsonDecoderTest` — round-trips an `Instant` through the custom deserializer
  and verifies date components.
- `JsonEncoderTest` — verifies strings pass through unmodified and objects
  produce the same output as raw `GsonEncoder`.
- `GenericServerResponseTest` — builder defaults, success flag, message field.

## Build

Published as a Maven artifact via `maven-publish`. Version from `JAR_VERSION`
env var. Dependencies: `:game-app:domain-data`, `:lib:java-extras`. Test
dependency: `:lib:test-common`.
