# TripleA

TripleA is an open-source, turn-based strategy game engine inspired by Axis & Allies.
It supports community-created maps, AI opponents, and online multiplayer via a lobby server.
The project has been active since 2002.

## Project Structure

This is a multi-module Gradle (Kotlin DSL) project. Key top-level directories:

| Directory | Purpose |
|-----------|---------|
| `game-app/` | Main application modules (game engine, UI, AI, server) |
| `http-clients/` | HTTP client libraries for server communication |
| `lib/` | Shared utility libraries (Swing helpers, websockets, XML parsing) |
| `docs/` | Project documentation (development, map-making, infrastructure) |
| `.build/` | Checkstyle, PMD configs, and code convention checks |
| `gradle/build-logic/` | Custom Gradle convention plugins |

### Module Dependency Overview

```
game-app/game-headed ──→ game-core, ai, domain-data, map-data, lobby-client, lib/feign-common, lib/java-extras, lib/swing-lib, lib/websocket-client
game-app/game-headless ─→ game-core, ai, domain-data, lobby-client, lib/java-extras
game-app/ai ───────────→ game-core, lib/java-extras
game-app/game-core ────→ domain-data, map-data, game-relay-server, lobby-client, lib/java-extras, lib/swing-lib, lib/websocket-client, lib/xml-reader
game-app/game-relay-server → lib/websocket-client, lib/websocket-server
http-clients/lobby-client → domain-data, lib/feign-common, lib/java-extras, lib/websocket-client
```

## Build & Development

- **Java version**: JDK 21
- **Build tool**: Gradle with Kotlin DSL, configuration cache enabled
- **Formatting**: Google Java Format via Spotless (`./gradlew spotlessApply`)
- **Static analysis**: Checkstyle (`.build/checkstyle.xml`) + PMD (`.build/pmd.xml`)

### Common Commands

```bash
# Build and run the desktop client
./gradlew :game-app:game-headed:run

# Run all checks (formatting + tests + static analysis + custom checks)
./verify

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :game-app:game-core:test

# Run a specific test class
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest

# Apply formatting
./gradlew spotlessApply
```

## Code Conventions

- Follows [Google Java Style](https://google.github.io/styleguide/javaguide.html)
- Uses **Lombok** for boilerplate reduction (`@Getter`, `@Builder`, etc.)
- Prefer `Optional` return types over returning `null`; annotate unavoidable nulls with `@Nullable`
- Avoid boolean method parameters — use overloads, enums, or factory methods
- Favor constructor injection, avoid setters
- Favor immutability — avoid mutating variables and parameters
- Use step-down method ordering (callers above callees)
- Define variables close to their usage
- Use `var` when the variable name makes the type obvious
- Use `List.of()`, `Map.of()`, `Set.of()` over `Collections.*` methods
- Refer to objects by interfaces (e.g., `Collection<>` not `ArrayList<>`)
- TODO comments must include a tracking token (e.g., `TODO: Issue#1234`)

## Testing

- JUnit 5 with Hamcrest `assertThat` assertions (preferred for new tests)
- Mockito for mocking
- JUnit assertions and Hamcrest matchers are the primary assertion styles; AssertJ is available but rarely used
- WireMock for HTTP stubbing
- Test fixtures shared from `:game-app:game-core` via `testFixtures` — use `TestMapGameData` enum and `TestMapGameDataLoader` to load test map data

## Architecture Concepts

The engine is built around these core abstractions (see `docs/development/engine-code-overview.md`):

- **GameData**: Central game state object, parsed from XML map files. Contains territories, players, units, resources, production rules, and relationships.
- **Delegates**: Encapsulate logic for one step of a game turn (e.g., movement, combat, purchasing, placement). Delegates communicate through `DelegateBridge` and use `ChangeFactory` to create state changes.
- **GamePlayer**: Handles user interaction — can be a human GUI, AI, or network player.
- **Changes**: All game state mutations go through `Change` objects created by `ChangeFactory`, applied via `DelegateBridge.addChange()`. This enables network synchronization.
- **Attachments**: Extensible name-value pairs attached to game entities (units, territories, players) that allow maps to customize game rules.

### Important Compatibility Constraints

- **Save-game compatibility**: Do NOT rename or delete private fields of classes extending `GameDataComponent`, and do NOT change their packages. Game saves use Java serialization.
- **Network compatibility**: Methods annotated `@RemoteActionCode` (in remote delegate interfaces like `IBattleDelegate`, `IMoveDelegate`, etc.) are invoked over the network. Their API signatures must not change.

## Key Domain Terms

- **Node**: Player name + IP address/port of a human player
- **PlayerId**: A playable side/faction in a map
- **Map**: Collection of files (images, polygons, properties, game XMLs)
- **Game/Game XML**: A specific XML configuration within a map
- **Delegate**: Game logic for one step of a turn
- **Battle Round**: Both sides firing; a battle has multiple rounds
- **First Strike**: Units with `isFirstStrike` fire before the general phase
- **AA**: Anti-aircraft units that fire in a targeted phase before general combat
