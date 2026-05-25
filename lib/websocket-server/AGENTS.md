# lib/websocket-server

WebSocket server library providing message routing, session management, and
broadcast support. Used by `game-relay-server` to host game lobbies.

## Architecture

All classes are in `org.triplea.web.socket`. The layering is:

```
StandaloneWebsocketServer (extends org.java_websocket.server.WebSocketServer)
  └── GenericWebSocket          — lifecycle dispatch + bad-message protection
        └── WebSocketMessagingBus — listener registry, send/broadcast API
              ├── MessageBroadcaster — fan-out to all sessions (parallelStream)
              ├── MessageSender      — async single-session send (new thread per message)
              └── SessionSet         — concurrent session tracking, IP-based lookup/close
```

## Key Classes

### StandaloneWebsocketServer
Thin adapter over `org.java_websocket.server.WebSocketServer`. Delegates all
lifecycle events (`onOpen`, `onClose`, `onMessage`, `onError`) to a
`GenericWebSocket`. Constructor takes a `WebSocketMessagingBus` and a port
number. Call `shutdown()` to stop the server (handles `InterruptedException`).

### GenericWebSocket
Central lifecycle handler. Two modes of construction:

1. **Direct** — `new GenericWebSocket(messagingBus)` for use with
   `StandaloneWebsocketServer`.
2. **Static registry** — `GenericWebSocket.init(class, bus, banCheck)` +
   `getInstance(class)` to map a websocket endpoint class to its handler.
   Currently unused externally but the registry exists for JSR 356 endpoint
   support.

Handles incoming JSON with Gson. Malformed messages increment a per-IP bad
message counter (Caffeine cache, 30 s TTL). When the bad message count
**exceeds** `MAX_BAD_MESSAGES` (2) — i.e., after 3 bad messages from an IP —
all subsequent messages from that IP are silently dropped.

Supports an optional `Predicate<InetAddress>` ban check; banned sessions are
disconnected on open with a `NORMAL_CLOSURE` close reason.

### WebSocketMessagingBus
The core message routing hub. Provides:

- **Typed listeners** — `addMessageListener(MessageType<T>, Consumer<WebSocketMessageContext<T>>)`
  dispatches only matching message types by comparing `messageTypeId` strings.
- **Catch-all listeners** — `addMessageListener(Consumer<MessageEnvelope>)` receives
  every inbound envelope (used by game-relay-server to rebroadcast all messages).
- **Disconnect listeners** — `addSessionDisconnectListener(BiConsumer<..>)` fires
  when a session closes.
- **Send API** — `sendResponse(session, message)` for unicast,
  `broadcastMessage(message)` for fan-out to all tracked sessions.
- **Error handling** — `onError` logs with a UUID error-id and sends a
  `ServerErrorMessage` back to the client.

### WebSocketSession / WebSocketSessionAdapter
`WebSocketSession` is the internal session interface (`isOpen`,
`getRemoteAddress`, `close`, `sendText`, `getId`). `WebSocketSessionAdapter`
provides two factory methods:

- `fromWebSocket(WebSocket)` — for `org.java_websocket` (standalone server).
  Generates a random UUID as the session ID.
- `fromSession(Session)` — for `javax.websocket` (JSR 356 / container-managed).
  Uses `InetExtractor` to parse the IP from session user properties.

### SessionSet
Thread-safe session store backed by `ConcurrentHashMap.newKeySet()`. Supports:
- `put` / `remove` for lifecycle tracking
- `values()` — returns only open sessions (filters closed ones)
- `getSessionsByIp` / `closeSessionsByIp` — IP-based operations for ban
  enforcement on existing connections

### MessageBroadcaster / MessageSender
`MessageBroadcaster` fans out a `MessageEnvelope` to a collection of sessions
using `parallelStream`. Requires a concurrent-safe collection to avoid
`ConcurrentModificationException`.

`MessageSender` sends to a single session by serializing the envelope to JSON
with Gson and calling `session.sendText()`. Each send runs on a **new thread**
(fire-and-forget).

### InetExtractor
Utility to parse an `InetAddress` from a JSR 356 session's user properties map.
Handles the `/127.0.0.1:port` format the websocket library produces.

## Dependencies

- `lib:websocket-client` — `MessageEnvelope`, `MessageType`, `WebSocketMessage`
  (the shared message protocol)
- `lib:feign-common` — direct dependency
- `lib:java-extras` — `StringUtils`, `Interruptibles`
- `org.java_websocket` — standalone WebSocket server implementation
- `com.google.gson` — JSON serialization
- `com.github.benmanes.caffeine` — TTL cache for bad-message tracking

## Editing Guidelines

- Messages use the envelope system from `lib:websocket-client`. See that
  module's AGENTS.md for `MessageEnvelope` / `MessageType` details.
- `MessageSender` spawns a new thread per send. Keep this in mind for
  high-throughput scenarios.
- `SessionSet` relies on `ConcurrentHashMap.newKeySet()` — do not replace with
  a non-concurrent collection.
- The bad-message cache in `GenericWebSocket` is a `static` field shared across
  all instances. Changes affect all websocket endpoints in the same JVM.
