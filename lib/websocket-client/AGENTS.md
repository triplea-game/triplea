# lib/websocket-client

WebSocket client library built on Java 11+ `java.net.http.WebSocket` (stdlib, not
`org.java_websocket`). Provides `GenericWebSocketClient` for connecting to
WebSocket servers with JSON message serialization via Gson and type-safe
listener dispatch.

## Source Files

| File | Purpose |
|------|---------|
| `GenericWebSocketClient` | Public facade implementing `WebSocket` and `WebSocketConnectionListener`. Routes messages to typed listeners, manages error propagation |
| `WebSocket` | Public interface defining the client contract: connect, close, send, add listeners |
| `WebSocketConnection` | Internal connection manager. Handles `java.net.http.HttpClient` WebSocket lifecycle, message queuing, keep-alive pings |
| `WebSocketConnectionListener` | Internal callback interface between `WebSocketConnection` and `GenericWebSocketClient` |
| `MessageEnvelope` | Generic JSON envelope wrapping payload with a type ID for routing |
| `WebSocketMessage` | Marker interface for transmissible message types. Single method: `toEnvelope()` |
| `MessageType<T>` | Immutable type descriptor pairing a `messageTypeId` string with a payload `Class<T>`. Factory: `MessageType.of(MyMessage.class)` |
| `ServerErrorMessage` | Built-in message type for server-sent errors. Auto-registered on `connect()` |
| `WebsocketPaths` | Path constants: `GAME_CONNECTIONS`, `PLAYER_CONNECTIONS` |
| `WebSocketProtocolSwapper` | Converts HTTP/HTTPS URIs to WS/WSS before connection |

## Message Serialization (Envelope Pattern)

All messages are wrapped in `MessageEnvelope` with a type ID for routing:

**Outgoing:** `WebSocketMessage.toEnvelope()` -> `MessageEnvelope` -> `gson.toJson()` -> send string

**Incoming:** receive string -> `gson.fromJson(MessageEnvelope.class)` -> match `messageTypeId` against registered listeners -> `envelope.getPayload(payloadType)` deserializes to concrete type

**Design rule:** `WebSocketMessage` implementations should use Java primitives
only (String, int), not enums, for backward compatibility between server and
client versions.

## Connection Lifecycle

1. **Connect** — `GenericWebSocketClient.connect()` delegates to
   `WebSocketConnection`, which creates an `HttpClient` WebSocket with 5-second
   connect timeout. On failure, retries once after 1-second sleep.

2. **Message queuing** — Messages sent before `onOpen()` are queued in a
   synchronized list and flushed in order when the connection opens.

3. **Keep-alive** — Ping sender starts on connection open. Sends pings every
   10 seconds using a `ScheduledTimer`. Each ping retries up to 5 times with
   3-second backoff via `Retriable`.

4. **Close (client-initiated)** — `close()` sends `NORMAL_CLOSURE` with reason
   `"Client disconnect."`. Triggers `connectionClosedListeners`.

5. **Termination (server-initiated)** — Any close with a different reason
   triggers `connectionTerminatedListeners` with the reason string.

6. **Errors** — WebSocket errors are logged and forwarded to the `errorHandler`
   consumer. `ServerErrorMessage` (auto-registered) also routes server error
   messages to the same handler.

## Threading Model

- `messageReceived()` and `addListener()` are `@Synchronized` (Lombok) for
  thread-safe listener dispatch and registration.
- `sendMessage()` and `onOpen()` synchronize on the `queuedMessages` list to
  prevent race conditions during connection setup.
- Message callbacks arrive on the HTTP client's internal thread.
- Ping sender runs on a separate scheduled timer thread.

## Conventions

- **Lombok throughout** — `@Builder` on `GenericWebSocketClient` constructor,
  `@Synchronized` for concurrency, `@Slf4j` for logging, `@Value` on
  `MessageType`.
- **Message fragments accumulated** — `onText()` collects `CharSequence`
  fragments in a `StringBuilder` until `last=true`, then dispatches the
  complete message.
- **Client vs. terminated distinction** — `connectionClosed` (client asked to
  disconnect) and `connectionTerminated` (server dropped connection) are
  separate listener lists with different signatures.

## Gotchas

- Only one automatic retry on initial connection failure — no exponential
  backoff or configurable retry policy.
- Ping failure after 5 retries only logs a warning; disconnection is left to
  the server's idle timeout.
- `sendMessage()` silently catches and logs exceptions on `sendText()` — callers
  won't see send failures.
- The `errorHandler` receives both transport errors (WebSocket `onError`) and
  application-level errors (`ServerErrorMessage`). There is no way to
  distinguish them.

## Build

Published as a Maven artifact via `maven-publish`. Version from `JAR_VERSION`
env var. Only local dependency: `:lib:java-extras` (provides `Interruptibles`,
`Retriable`, `Timers`). Uses Java stdlib `java.net.http` for WebSocket — no
external WebSocket library.
