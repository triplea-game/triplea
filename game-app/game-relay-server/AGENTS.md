# game-relay-server

A WebSocket message relay that broadcasts messages between game clients during multiplayer sessions. Acts as a network bridge — receives messages from one client and re-broadcasts to all connected clients.

## Architecture

```
GameRelayServer
  └── StandaloneWebsocketServer (extends org.java_websocket.server.WebSocketServer)
      └── GenericWebSocket
          └── WebSocketMessagingBus (core message routing)
              ├── MessageBroadcaster (broadcasts to all clients)
              ├── MessageSender (sends to individual clients)
              └── SessionSet (tracks open WebSocket sessions)
```

**Single main class**: `org.triplea.game.server.GameRelayServer`
- Constructor takes a port number
- `start()` / `stop()` lifecycle methods
- `createLocalhostConnectionUri(int port)` — helper for creating connection URIs

## Functionality

- Accepts WebSocket connections and tracks active sessions
- Receives JSON-encoded `MessageEnvelope` objects (deserialized via Gson)
- Broadcasts incoming messages to all other connected clients
- Validates remote IP addresses for ban checking
- Tracks and rate-limits bad messages via Caffeine cache

## Dependencies

- `lib:websocket-client`, `lib:websocket-server`
- Uses Java WebSocket library (`org.java_websocket`)
