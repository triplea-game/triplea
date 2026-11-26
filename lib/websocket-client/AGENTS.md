# lib/websocket-client

WebSocket client library built on OkHttp (`okhttp3.WebSocket`). Provides
`GenericWebSocketClient` for connecting to WebSocket servers with JSON message
serialization via Gson and type-safe listener dispatch.

## Source Files

| File | Purpose |
|------|---------|
| `GenericWebSocketClient` | Public facade implementing `WebSocket` and `WebSocketConnectionListener`. Routes messages to typed listeners, manages error propagation |
| `WebSocket` | Public interface defining the client contract: connect, close, send, add listeners |
| `WebSocketConnection` | Internal connection manager. Handles the OkHttp `WebSocket` lifecycle, message queuing, reconnect; relies on OkHttp's native ping interval for keep-alive |
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
   `WebSocketConnection`, which opens an OkHttp `WebSocket` with a 5-second
   connect timeout. On failure, retries once after 1-second sleep.

2. **Message queuing** — Messages sent before `onOpen()` are queued in a
   synchronized list and flushed in order when the connection opens.

3. **Keep-alive** — Handled natively by OkHttp via `pingInterval` (30s). If a
   pong is not received the connection fails, which triggers a reconnect
   loop.

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
- Message callbacks arrive on OkHttp's internal reader thread.
- Reconnect runs on a dedicated virtual thread (`websocket-reconnect-thread`).

## Conventions

- **Lombok throughout** — `@Builder` on `GenericWebSocketClient` constructor,
  `@Synchronized` for concurrency, `@Slf4j` for logging, `@Value` on
  `MessageType`.
- **Whole-message delivery** — OkHttp reassembles frames and delivers each
  complete text message once via `onMessage(String)`.
- **Client vs. terminated distinction** — `connectionClosed` (client asked to
  disconnect) and `connectionTerminated` (server dropped connection) are
  separate listener lists with different signatures.

## Gotchas

- Only one automatic retry on *initial* connection failure (then `errorHandler`
  fires). After an established connection drops, the reconnect loop retries
  indefinitely with a fixed 5s back-off.
- `sendMessage()` logs (does not throw) if OkHttp refuses to enqueue a message —
  callers won't see send failures.
- An unexpected transport failure (`onFailure`) drives the reconnect loop rather
  than calling `handleError`; the `errorHandler` is reserved for initial-connect
  failure and application-level errors (`ServerErrorMessage`).

## Build

Published as a Maven artifact via `maven-publish`. Version from `JAR_VERSION`
env var. Local dependency: `:lib:java-extras` (provides `Interruptibles`).
External dependency: OkHttp (`libs.okhttp`) for the WebSocket transport.
