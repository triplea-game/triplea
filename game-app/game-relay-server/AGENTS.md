# game-relay-server

A WebSocket relay that carries game traffic between clients so a host can run a
game without inbound port-forwarding — host and clients all dial *out* to the
relay. One relay multiplexes **many games** by game-id and routes messages
within a game; it never inspects the game payload.

## Architecture

```
GameRelayServer
  └── StandaloneWebsocketServer (extends org.java_websocket.server.WebSocketServer)
      └── GenericWebSocket
          └── WebSocketMessagingBus
              └── GameRelayRouter (per-game membership + routing + host + boot/ban)
```

Main classes (`org.triplea.game.server`):
- `GameRelayServer` — constructor takes a port (or bind address); `start()`/`stop()`;
  `createLocalhostConnectionUri(int port)`.
- `RelayServerMain` — standalone `public static void main` entrypoint for a
  lobby-managed relay process. Port/bind resolved by precedence: CLI arg →
  system property (`triplea.relay.port` / `triplea.relay.bindAddress`) → env
  (`TRIPLEA_RELAY_PORT` / `TRIPLEA_RELAY_BIND_ADDRESS`) → default 6000, wildcard bind.
- `relay.GameRelayRouter` — the routing core: per-game session groups, host
  designation, boot/ban, disconnect cleanup.
- `relay.GameRelayEnvelope` — the routing frame `{gameId, to, from, correlationId,
  type, payload}` (a `WebSocketMessage` packed into a `MessageEnvelope`; `payload`
  is opaque to the relay).
- `relay.RelayControl` — the relay-owned control records (JOIN/WELCOME/MEMBER_*/BOOT).

## Functionality

- Accepts WebSocket connections; on `JOIN{gameId, playerName}` assigns a stable
  UUID nodeId, tracks session↔nodeId↔gameId, and marks the **first** node in a
  game as its **host** (host-only boot/ban). Replies `WELCOME{nodeId, host, members}`.
- Routes a data message (`type="GAME"`): `to==null` → broadcast to all *other*
  members of that gameId (sender excluded); else direct to that nodeId. Never
  crosses game-ids. Re-stamps `gameId`/`from` authoritatively; never decodes `payload`.
- Notifies members of membership changes (`MEMBER_JOINED`/`MEMBER_LEFT`); drops
  empty games; cleans up on disconnect.
- `BOOT{targetNodeId, ban}` from the host closes the target session (and IP-bans
  via the `GenericWebSocket` ban hook when `ban=true`).
- Rate-limits malformed messages via the Caffeine bad-message cache.

Consumed by the websocket transport in game-core
(`games.strategy.net.websocket.transport`, behind `ClientSetting.useWebsocketTransport`).

Not yet implemented (round 2): host reassignment on host disconnect; a real
join-auth/session model for a shared public relay (currently admits all).

## Dependencies

- `lib:websocket-client`, `lib:websocket-server`
- Java WebSocket library (`org.java_websocket`)
