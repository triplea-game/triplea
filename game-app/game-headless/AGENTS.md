# game-headless

A headless (no GUI) game server, commonly called a "bot". It hosts multiplayer games and connects to the TripleA lobby so players can find and join games.

## Entry Point

**Main class**: `org.triplea.game.server.HeadlessGameRunner`

Startup sequence:
1. Initializes locale, client settings, and product version
2. Configures `LobbyHttpClientConfig` with system ID and version
3. Validates `MAPS_FOLDER` environment variable
4. Validates required system properties
5. Extracts/unzips map files from the maps folder
6. Starts `HeadlessGameServer`

## Required Configuration (System Properties / Env Vars)

| Property | Description |
|----------|-------------|
| `MAPS_FOLDER` (env) | Path to game maps directory |
| `triplea.name` | Bot name (must start with "Bot", min 7 chars) |
| `triplea.port` | Server port (> 0) |
| `triplea.lobby.uri` | Lobby server URL |
| `triplea.server` | Set to "true" for server mode |

## Package Structure

- **`org.triplea.game.server`** — Core server logic
  - `HeadlessGameRunner` — Entry point
  - `HeadlessGameServer` — Manages game instances and lobby connection
  - `HeadlessServerSetup` / `HeadlessServerSetupModel` — Server setup and connection handling
  - `HeadlessLaunchAction` — Game launch orchestration
  - `HeadlessServerStartupRemote` — Remote API for game management
- **`org.triplea.game.server.debug`** — `ChatAppender` (Logback appender that sends logs to in-game chat)

## Architecture

```
HeadlessGameRunner
  → HeadlessGameServer (orchestrates game lifecycle)
    → ServerGame (from game-core)
    → Lobby connection (via lobby-client)
```

## Dependencies

- `game-app:game-core`, `game-app:ai`, `game-app:domain-data`
- `http-clients:lobby-client`
- `lib:java-extras`

## Build

Uses Shadow JAR plugin to create a fat JAR. Docker image published to `ghcr.io/triplea-game/bot:latest` via CI.
