# game-app

The main application layer of TripleA, organized as several Gradle submodules.

## Module Map

| Module | Purpose |
|--------|---------|
| `game-core` | Core game engine: data model, delegates, networking, UI components |
| `game-headed` | Desktop GUI client (Swing) — the main user-facing application |
| `game-headless` | Headless server ("bot") for hosting multiplayer games without a GUI |
| `ai` | AI player implementations (FlowField algorithm, DoesNothing fallback) |
| `domain-data` | Shared domain value objects used across client and server (UserName, ApiKey, etc.) |
| `map-data` | Map file parsing — XML game data and YAML map metadata |
| `game-relay-server` | WebSocket relay server that broadcasts messages between game clients |
| `smoke-testing` | Save game compatibility tests — downloads save files and verifies they load correctly |

## Dependency Flow

```
game-headed ──→ game-core, ai, domain-data, map-data, lobby-client
game-headless ─→ game-core, ai, domain-data, lobby-client, lib:java-extras
ai ───────────→ game-core, lib:java-extras
game-core ────→ domain-data, map-data, game-relay-server, lobby-client
game-relay-server → lib:websocket-client, lib:websocket-server
```

## Shared Conventions

All modules use the `triplea-java-library` convention plugin (from `gradle/build-logic`), which applies:
- Java 21 compilation
- Google Java Format via Spotless

Additionally, the root `build.gradle.kts` applies to all subprojects:
- Checkstyle and PMD static analysis
- JaCoCo code coverage
