# game-core

The core game engine module. Contains the generic turn-based strategy engine framework, the TripleA-specific game implementation, and the Swing-free UI support (map geometry, display text, headless display) shared by the headed and headless clients. Swing UI lives in `game-headed`.

## Package Structure

### `games.strategy.engine` — Generic Game Engine
- **`data/`** — Game data model: `GameData` (central state), `Territory`, `Unit`, `GamePlayer`, `GameMap`, `Route`, `Resource`, `UnitType`, `GameSequence`, `Change`; also `data/properties/GameProperties`
- **`delegate/`** — Delegate framework: `IDelegate`, `IDelegateBridge`, `IPersistentDelegate`. Delegates encapsulate logic for one step of a game turn.
- **`framework/`** — Game lifecycle, startup, map management (see [Engine Framework](#engine-framework) below for details)
- **`player/`** — Player abstraction: `Player` interface, `PlayerBridge`
- **`message/`** — Network messaging and remote method invocation (see [Messaging Layer](#messaging-layer) below for details)
- **`random/`** — Dice rolling and random number generation
- **`history/`** — Game history (`History`) and undo support
- **`posted/`** — Play-by-email (PBEM) and play-by-forum (PBF) support

### `games.strategy.triplea` — TripleA Implementation
- **`delegate/`** — Game phase delegates (see below for details); also `delegate/battle/` (see `delegate/battle/AGENTS.md` for the battle subsystem — step-based combat resolution, casualty selection, retreat logic)
  - **Hierarchy:** `IDelegate` → `AbstractDelegate` → `BaseTripleADelegate` (fires `TriggerAttachment` before/after each step). Persistent delegates use `BasePersistentDelegate` instead.
  - **Setup:** `InitializationDelegate` (one-time game init — original owners, tech, transported units), `RandomStartDelegate` (Risk-style random territory assignment)
  - **Movement:** `MoveDelegate` (validate/execute unit moves, undo stack), `SpecialMoveDelegate` (paratroopers/airborne from airbases)
  - **Combat:** `BattleDelegate` (orchestrate all battles via `BattleTracker`, scrambling, rockets, bombardment)
  - **Production:** `PurchaseDelegate` (buy units/repairs with resources), `PlaceDelegate` (place produced units), `TechnologyDelegate` (tech research rolls), `TechActivationDelegate` (apply researched tech advances)
  - **Diplomacy:** `PoliticsDelegate` (political actions, relationship changes, alliance chaining), `UserActionDelegate` (custom user-defined actions)
  - **End of turn:** `EndTurnDelegate` (income collection, national objectives, convoy blockades), `EndRoundDelegate` (round-end bookkeeping)
  - **Administration:** `EditDelegate` (persistent — edit game state: add/remove units, change ownership, modify resources)
  - **Variants:** `BidPurchaseDelegate`, `BidPlaceDelegate` (auction mode), `NoPuPurchaseDelegate`, `NoPuEndTurnDelegate` (non-PU games), `NoAirCheckPlaceDelegate` (skip air landing validation)
- **`delegate/remote/`** — Remote interfaces for network play: `IMoveDelegate`, `IBattleDelegate`, `IPurchaseDelegate`, `IAbstractPlaceDelegate`, `ITechDelegate`, `IPoliticsDelegate`, `IUserActionDelegate`, `IEditDelegate`. Each method annotated with `@RemoteActionCode(n)` for serialization.
- **`ai/`** — AI support (~46 files): `AbstractAi` (base class for all AI players). `ai/pro/` contains the default strategic AI — `ProAi` (hard) and its base `AbstractProAi`, plus supporting data models (`ProData`), simulation, logging, and utility classes. `ai/fast/FastAi` (medium, extends `AbstractProAi` with shared calculator). `ai/weak/WeakAi` (easy, legacy AI). The `ai` module (separate from this package) adds FlowFieldAi and DoesNothingAi.
- **`attachments/`** — Unit/territory/player attachments that customize game rules per map
- **`formatter/`** — Display formatting for game data
- **`odds/calculator/`** — Battle odds simulation
- **`Properties.java`** — Game property constants (single file, not a package)
- **`settings/`** — Client settings management
- **`ui/`** — Swing-free UI support shared with game-headless (see [UI Layer](#ui-layer) below); the Swing gameplay UI lives in game-headed
- **`util/`** — Various utility classes

### `org.triplea` — Modern TripleA Modules
- **`game/`** — Client startup, chat integration
- **`sound/`** — Sound/audio management
- **`debug/`** — Debug and error reporting; the error dialog, its logback appender, and the report Swing views live in game-headed

### `tools.util` — Map Utilities
- `ToolsUtil` — territory-name helpers shared with `MapData`; the map-making tools themselves live in `game-headed`

## Key Abstractions

- **GameData**: The central state object. All game state flows through this — territories, units, players, resources, production rules, relationships, and game properties.
- **Delegates**: Each game phase has a delegate with a `start()`/`end()` lifecycle. Delegates are defined in game XML, instantiated by reflection, and connected to the engine via `IDelegateBridge`. They mutate state only via `Change` objects through `bridge.addChange()`. Each delegate exposes an `IRemote` interface (e.g., `IMoveDelegate`, `IBattleDelegate`) with `@RemoteActionCode`-annotated methods for network play. Delegates persist state via `saveState()`/`loadState()` for save games.
- **Change / ChangeFactory**: All state mutations are represented as `Change` objects. This enables network synchronization and undo/redo.
- **Attachments**: Name-value pairs attached to units, territories, players, or resources. Maps use these to define custom rules (attack values, movement points, special abilities). See [Attachment System](#attachment-system) below for details.

## Attachment System

The `attachments/` package (~21 files) implements the property system that lets map XML define game rules — unit stats, territory effects, tech bonuses, political actions, victory conditions, and triggers.

### Class Hierarchy

```
IAttachment (interface, in engine/data/)
└── DefaultAttachment (abstract, in engine/data/)
    ├── UnitAttachment
    ├── TerritoryAttachment
    ├── PlayerAttachment
    ├── TechAttachment
    ├── CanalAttachment
    ├── TerritoryEffectAttachment
    ├── RelationshipTypeAttachment
    ├── UnitSupportAttachment
    ├── TechAbilityAttachment
    └── AbstractConditionsAttachment (implements ICondition)
        ├── AbstractRulesAttachment
        │   └── AbstractPlayerRulesAttachment
        │       └── RulesAttachment
        ├── AbstractTriggerAttachment
        │   └── TriggerAttachment
        └── AbstractUserActionAttachment
            ├── PoliticalActionAttachment
            └── UserActionAttachment
```

### Base Classes

- **`IAttachment`** (in `engine/data/`) — Interface extending `Serializable` and `DynamicallyModifiable`. Methods: `validate()`, `getAttachedTo()`, `setAttachedTo()`, `getName()`.
- **`DefaultAttachment`** (in `engine/data/`) — Abstract base. Provides XML string parsing utilities (`getInt()`, `getBool()`, `splitOnColon()`), null-safe collection accessors (`getListProperty()`, `getSetProperty()`, `getMapProperty()`, `getIntegerMapProperty()`), and property access via `getPropertyOrEmpty()`. Collections default to `null` for memory optimization; getters return unmodifiable empty collections when null.
- **`AbstractConditionsAttachment`** — Adds condition evaluation: `conditions` (list of `RulesAttachment`), `conditionType` (AND/OR/numeric range like "2-4"), `invert`, `chance` ("x:y" format). Uses memoized recursive evaluation via `testAllConditionsRecursive()`.
- **`AbstractRulesAttachment`** — Adds territory-based condition evaluation. `getTerritoryListBasedOnInputFromXml()` parses keywords: "controlled", "original", "enemy", "map", "all", etc.
- **`AbstractPlayerRulesAttachment`** — Adds placement/movement restriction properties.
- **`AbstractTriggerAttachment`** — Adds trigger lifecycle: usage limits, round tracking, notification text, `when` (before/after a game step), chance testing.
- **`AbstractUserActionAttachment`** — Adds user-actionable properties: cost (PUs/resources), attempt limits per turn, action acceptance by players.

### Concrete Attachment Classes

**`UnitAttachment`** — Attaches to `UnitType`. The largest attachment (~1000+ lines). Key property groups:
- **Movement**: `movement`, `isAir`, `isSea`, `canBlitz`, `fuelCost`, `movementLimit`, `canNotMoveDuringCombatMove`
- **Combat**: `attack`, `defense`, `attackRolls`, `defenseRolls`, `isMarine`, `artillery`, `canBombard`, `bombardStrength`
- **First-strike/evasion**: `isFirstStrike`, `canEvade`, `canNotTarget`, `canNotBeTargetedBy`, `isDestroyer`
- **Transport**: `transportCapacity`, `transportCost`, `carrierCapacity`, `carrierCost`, `isAirTransport`, `isLandTransport`
- **AA**: `attackAa`, `offensiveAttackAa`, `maxAaAttacks`, `maxRoundsAa`, `typeAa`, `targetsAa`, `isAaForCombatOnly`, `isAaForBombingThisUnitOnly`
- **Bombing**: `isStrategicBomber`, `bombingMaxDieSides`, `bombingBonus`, `bombingTargets`
- **Damage/HP**: `hitPoints`, `canBeDamaged`, `maxDamage`, `maxOperationalDamage`
- **Production**: `canProduceUnits`, `canProduceXUnits`, `createsUnitsList`, `createsResourcesList`
- **Construction**: `isConstruction`, `constructionType`, `requiresUnits`, `consumesUnits`

**`TerritoryAttachment`** — Attaches to `Territory`. Properties:
- **Production/ownership**: `capital`, `production`, `unitProduction`, `originalOwner`, `originalFactory`
- **Territory type**: `isImpassable`, `navalBase`, `airBase`, `kamikazeZone`, `blockadeZone`, `convoyRoute`
- **Effects**: `territoryEffect` (list), `changeUnitOwners`, `captureUnitOnEnteringBy`, `whenCapturedByGoesTo`
- **Resources**: `resources` (ResourceCollection), `victoryCity`
- Key static: `getFirstOwnedCapitalOrFirstUnownedCapital()`

**`PlayerAttachment`** — Attaches to `GamePlayer`. Properties: `vps`, `captureVps`, `retainCapitalNumber`, `giveUnitControl`, `shareTechnology`, `suicideAttackResources`, `suicideAttackTargets`, `placementLimit`, `movementLimit`, `attackingLimit`.

**`RulesAttachment`** — Attaches to `GamePlayer`. Extends `AbstractPlayerRulesAttachment`. Complex condition checking:
- **Tech conditions**: `techs`, `techCount`
- **Territory ownership**: `alliedOwnershipTerritories`, `directOwnershipTerritories`, `alliedExclusionTerritories`, `directExclusionTerritories`, `enemyExclusionTerritories`
- **Presence**: `directPresenceTerritories`, `alliedPresenceTerritories`, `enemyPresenceTerritories`, `unitPresence`
- **Other**: `relationship`, `isAI`, `atWarPlayers`, `destroyedTuv`, `battle`
- Key static: `getNationalObjectives()` filters by "objectiveAttachment" prefix

**`TriggerAttachment`** — Attaches to `GamePlayer`. Extends `AbstractTriggerAttachment`. The event-driven rules engine. Can modify at runtime:
- **Production**: `frontier`, `productionRule`, `purchase`
- **Technology**: `tech`, `availableTech`
- **Units**: `placement` (territory → unit map), `removeUnits`
- **Properties**: `unitProperty`, `territoryProperty`, `playerProperty`, `relationshipTypeProperty`, `territoryEffectProperty` (each with corresponding `*AttachmentName`)
- **Resources**: `resource`, `resourceCount`
- **Relationships**: `relationshipChange`
- **Victory**: `victory`
- **Chaining**: `activateTrigger` (fires other triggers)
- Key statics: `collectAndFireTriggers()`, `collectForAllTriggersMatching()`

**`TechAttachment`** — Attaches to `GamePlayer`. Hardcoded tech flags: `heavyBomber`, `longRangeAir`, `jetPower`, `rocket`, `industrialTechnology`, `superSub`, `destroyerBombard`, `improvedArtillerySupport`, `paratroopers`, `warBonds`, `mechanizedInfantry`, `aaRadar`, `shipyards`. Also `genericTech` (String→Boolean map), `techCost`.

**`TechAbilityAttachment`** — Attaches to `TechAdvance`. Defines bonuses granted by technologies:
- **Combat bonuses**: `attackBonus`, `defenseBonus`, `attackRollsBonus`, `defenseRollsBonus`, `bombingBonus` (all `IntegerMap<UnitType>`)
- **Movement**: `movementBonus`
- **Production**: `productionBonus`, `repairDiscount`, `minimumTerritoryValueForProductionBonus`
- **Rockets**: `rocketDiceNumber`, `rocketDistance`, `rocketNumberPerTerritory`
- **Airborne**: `airborneForces`, `airborneCapacity`, `airborneTypes`, `airborneDistance`, `airborneBases`
- **Abilities**: `unitAbilitiesGained` (UnitType → Set of ability names like `ABILITY_CAN_BLITZ`, `ABILITY_CAN_BOMBARD`)

**`CanalAttachment`** — Attaches to `Territory`. Properties: `canalName`, `landTerritories` (which land territories the canal connects), `excludedUnits`. Validation requires exactly 2 sea zones with the same canal name.

**`TerritoryEffectAttachment`** — Attaches to `TerritoryEffect`. Properties: `combatDefenseEffect`, `combatOffenseEffect` (both `IntegerMap<UnitType>`), `movementCostModifier`, `noBlitz`, `unitsNotAllowed`.

**`RelationshipTypeAttachment`** — Attaches to `RelationshipType`. Defines relationship archetypes (War, Allied, Neutral) and behavioral flags: `canMoveLandUnitsOverOwnedLand`, `canTakeOverOwnedTerritory`, `canMoveThroughCanals`, `alliancesCanChainTogether`, `upkeepCost`, etc. Uses `PROPERTY_DEFAULT`/`PROPERTY_TRUE`/`PROPERTY_FALSE` values.

**`UnitSupportAttachment`** — Attaches to `UnitType`. Defines support bonuses one unit gives to others: `bonus` (magnitude), `bonusType`, `unitType` (supported types), `dice`/`side`, `allied`/`enemy`, `number`, `impArtTech` (requires improved artillery tech). Has `PropertyName` enum defining 15 mutable properties.

**`PoliticalActionAttachment`** — Attaches to `GamePlayer`. Extends `AbstractUserActionAttachment`. Defines political actions with `relationshipChange` (format: "player1:player2:relationshipType").

**`UserActionAttachment`** — Attaches to `GamePlayer`. Extends `AbstractUserActionAttachment`. Defines custom player actions that can fire triggers via `activateTrigger`.

### Utility Classes

- **`FireTriggerParams`** — Value object for trigger firing parameters (before/after, step name, use/test flags).
- **`UnitTypeComparator`** — Sorts unit types by infrastructure, AA, air, sea, attack power, then name.

### XML Loading & Property Parsing

Attachments are loaded from game XML by the parser:
1. Parser reads attachment elements (name, type, options)
2. Instantiates the attachment class, calls `setOptions()` to populate properties
3. Property setters parse colon-delimited strings (`splitOnColon()`), hyphen-delimited ranges, booleans, integers
4. After all attachments loaded, `validate()` is called on each
5. Attachments stored on `Attachable` objects (Territory, UnitType, GamePlayer, etc.) in a `Map<String, IAttachment>`

**Property naming convention**: setter `setPropertyName(String)` for XML parsing, getter `getPropertyName()`, reset `resetPropertyName()`.

### Retrieval Patterns

Each concrete attachment provides a static `get()` method:
- `UnitAttachment.get(UnitType)` — direct lookup
- `TerritoryAttachment.get(Territory)` — returns Optional
- `TriggerAttachment.getTriggers(GamePlayer, Predicate)` — filtered collection
- `RulesAttachment.getNationalObjectives(GamePlayer)` — prefix-filtered ("objectiveAttachment")

## Test Fixtures

This module exposes **test fixtures** used by other modules (notably `ai` and `smoke-testing`). Key test support:
- `TestMapGameData` enum — loads predefined test maps
- `TestMapGameDataLoader` — utility to load map XML for tests
- Test map XMLs in `src/testFixtures/resources/`

## Compatibility Warnings

- **Save games**: Do NOT rename/delete private fields or change packages of `GameDataComponent` subclasses (Java serialization).
- **Network**: Methods annotated `@RemoteActionCode` (in remote delegate interfaces like `IBattleDelegate`, `IMoveDelegate`) are called over the network — do not change their signatures or method numbers.

## Messaging Layer

The `message/` package implements transparent remote method invocation (RMI) over a hub-spoke network topology. It provides two communication patterns: **Remotes** (one-to-one RPC with return values) and **Channels** (one-to-many broadcast, fire-and-forget).

### Architecture Overview

```
┌─────────────────┐      ┌──────────────────────────────┐      ┌─────────────────┐
│  Client (Spoke)  │◄────►│  Server (Hub)                │◄────►│  Client (Spoke)  │
│                  │      │                              │      │                  │
│ UnifiedMessenger │      │ UnifiedMessenger              │      │ UnifiedMessenger │
│ RemoteMessenger  │      │ + UnifiedMessengerHub         │      │ RemoteMessenger  │
│ ChannelMessenger │      │ RemoteMessenger               │      │ ChannelMessenger │
└─────────────────┘      │ ChannelMessenger              │      └─────────────────┘
                          └──────────────────────────────┘
```

- **`UnifiedMessenger`** — Core engine for both remotes and channels. Manages local endpoints, sends `HubInvoke` messages to the server, and processes incoming `SpokeInvoke`/`SpokeInvocationResults`. On the server node, it also creates a `UnifiedMessengerHub`.
- **`UnifiedMessengerHub`** (server only) — Routes invocations between spokes. Tracks which nodes have endpoint implementors and forwards calls accordingly.
- **`RemoteMessenger`** — One-to-one RPC. `getRemote(name)` returns a Java dynamic proxy; method calls on the proxy are serialized and sent to the single remote implementor, blocking until results return.
- **`ChannelMessenger`** — One-to-many broadcast. `getChannelBroadcaster(name)` returns a proxy that multicasts to all subscribers. Returns immediately (no return value).
- **`Messengers`** (`games.strategy.net`) — Convenience facade combining `IMessenger`, `IRemoteMessenger`, and `IChannelMessenger`. Used throughout the codebase as the primary messaging API.

### How Remote Invocation Works

1. **Proxy creation**: `RemoteMessenger.getRemote()` / `ChannelMessenger.getChannelBroadcaster()` create a Java `Proxy` with `UnifiedInvocationHandler`.
2. **Method interception**: The handler validates all arguments are `Serializable`, creates a `RemoteMethodCall` (endpoint name + method number from `@RemoteActionCode` + args), and sends a `HubInvoke` to the server.
3. **Hub routing**: `UnifiedMessengerHub` looks up which node(s) have implementors for the endpoint, wraps the call in a `SpokeInvoke`, and forwards it.
4. **Execution**: The target `UnifiedMessenger` retrieves the local `EndPoint`, takes a sequence number for ordering, and executes via thread pool (15 threads).
5. **Result return**: Results flow back as `HubInvocationResults` → hub → `SpokeInvocationResults` → caller. The caller blocks on a `CountDownLatch` until results arrive.

For channels (fire-and-forget), step 5 is skipped — `needReturnValues=false` and the caller returns immediately.

### `@RemoteActionCode` and Serialization

`RemoteMethodCall` implements `Externalizable` for compact binary encoding:
- Method identity is a **single byte** (the `@RemoteActionCode` value), not the method name string.
- Argument types are elided when they match the expected parameter types.
- On deserialization, `RemoteMethodCall.resolve(Class)` reconstructs the `Method` object from the method number.

**Never change** a `@RemoteActionCode` value or reuse a number within the same interface — this breaks network compatibility between different client versions.

### Endpoint Lifecycle

- **Registration**: `RemoteMessenger.registerRemote()` / `ChannelMessenger.registerChannelSubscriber()` → `UnifiedMessenger.addImplementor()` → creates local `EndPoint` → sends `HasEndPointImplementor` to hub.
- **Unregistration**: Removes implementor from `EndPoint`; if none remain, sends `NoLongerHasEndPointImplementor` to hub.
- **`EndPoint`** manages a set of local implementors. For single-threaded endpoints (channels), it enforces execution order via sequential numbering (`takeANumber()` / `waitTillCanBeRun()` / `releaseNumber()`).

### Message Type Hierarchy

```
Serializable
├── RemoteMethodCall              (method number + args)
├── RemoteMethodCallResults       (return value or exception)
├── Invoke (abstract)
│   ├── HubInvoke                 (spoke → hub)
│   └── SpokeInvoke               (hub → spoke)
├── InvocationResults (abstract)
│   ├── HubInvocationResults      (spoke → hub)
│   └── SpokeInvocationResults    (hub → spoke)
├── HasEndPointImplementor        (endpoint registration)
└── NoLongerHasEndPointImplementor (endpoint removal)
```

### Threading and Synchronization

- **`endPointMutex`**: Guards the `localEndPoints` map (brief holds for register/unregister).
- **`pendingLock`**: Guards `pendingInvocations` and `results` maps (brief holds for storing/retrieving results).
- **`numberMutex`** (in `EndPoint`): Enforces sequential execution order for single-threaded endpoints (channels).
- **Thread pool**: 15-thread fixed pool executes incoming `SpokeInvoke` messages asynchronously.
- **EDT warning**: `UnifiedInvocationHandler` logs a warning if a blocking network call is made from Swing's Event Dispatch Thread.
- **Connection loss**: `messengerInvalid()` fails all pending invocations with the connection error, unblocking waiting threads.

### Network Transport Layer (`games.strategy.net`)

The messaging layer sits on top of a lower-level network transport:
- **`IMessenger`** — Send `Serializable` messages between nodes. Delivers messages in order per connection.
- **`INode`** / **`Node`** — Identity of a network participant (address + port + display name).
- **`ServerMessenger`** / **`ClientMessenger`** — Server and client implementations using NIO sockets.
- **`nio/`** — NIO socket implementation: `NioSocket`, `NioReader`, `NioWriter`, `Encoder`, `Decoder`.
- **`QuarantineConversation`** — Login/authentication handshake before a client is accepted.
- **`LocalNoOpMessenger`** — Stub for single-player (no network).

## Engine Framework

The `framework/` package (~110 files) manages the game lifecycle — from startup and game selection through network play to save/load. It contains the core `IGame` hierarchy, the startup subsystem, map download management, and supporting UI utilities.

### IGame Hierarchy (Core Lifecycle)

```
IGame (interface)
└── AbstractGame (shared state: GameData, Messengers, Vault, PlayerManager)
    ├── ServerGame (delegate execution, auto-save, random source, step advancement)
    └── ClientGame (syncs state from server via IGameModifiedChannel)
```

- **`IGame`** — Master interface for a running game. Provides access to `GameData`, `Messengers`, `Vault`, `PlayerManager`, random source, and display/sound channels.
- **`AbstractGame`** — Base implementation managing shared state, player bridge setup, and display/sound channel registration.
- **`ServerGame`** — Runs on the host. Executes delegates via `DelegateExecutionManager`, manages auto-save, provides random number generation (regular and delegate-specific), exposes `IServerRemote` for save-game retrieval.
- **`ClientGame`** — Runs on remote clients. Implements `IGameModifiedChannel` to receive state changes broadcast by the server. Step advancement is handled via `IGameStepAdvancer` remote interface.

### Key Framework Classes

- **`GameDataManager`** — Serializes/deserializes game data to GZIP-compressed save files. Handles delegate state, with options for partial serialization (skip history, attachments).
- **`IGameLoader`** — Interface for game-specific initialization: `newPlayers()` creates player instances, `startGame()` begins gameplay.
- **`IGameModifiedChannel`** — Broadcast channel for all state changes: `gameDataChanged(Change)`, `stepChanged(...)`, history events.
- **`GameDataUtils`** — Serialization utilities: deep clone, byte array conversion, object translation between `GameData` instances.
- **`GameRunner`** — Static constants: `PORT` (3300), `TRIPLEA_HEADLESS` property, `BOT_GAME_HOST_COMMENT`.
- **`LocalPlayers`** — Tracks which `Player` instances are local to the current node (human or AI).
- **`GameState`** — Simple state holder tracking whether the game has started.
- **`GameShutdownRegistry`** — Manages JVM shutdown hooks.
- **`CliProperties`** — Command-line system property names.
- **`VerifiedRandomNumbers`** — Random number verification/tracking.
- **`AutoSaveFileUtils`** / **`HeadlessAutoSaveFileUtils`** — Auto-save file path management for headed/headless modes.
- **`I18nEngineFramework`** / **`I18nResourceBundle`** — Internationalization support.

### Startup Subsystem (~47 files)

The `startup/` package manages everything from game selection through network setup to game launch.

#### Launchers (`startup/launcher/`)

- **`ILauncher`** — Single-method interface: `launch()`.
- **`ServerLauncher`** — Network host. Establishes a `GameRelayServer` on port 6000 for P2P relay, uses `CryptoRandomSource`, tracks remote client connections, optionally integrates with the in-game lobby watcher.
- **`LaunchAction`** — Enum of launch modes.
- **`MapNotFoundException`** — Thrown when map files are unavailable.

#### Server/Client Models (`startup/mc/`)

- **`ServerModel`** — Network server state machine. Manages `ServerMessenger`, player-to-node mappings, player enable/disable states, chat, moderator actions, and lobby watcher connections. Creates `ServerLauncher` when all clients are ready.
- **`ClientModel`** — Network client state machine. Connects via `ClientMessenger`, implements `IMessengerErrorListener`, creates `ClientGame` with remote player data, detects headless bot servers.
- **`GameSelector`** — Game selection state holder.
- **`IClientChannel`** / **`IServerStartupRemote`** — Remote interfaces for client/server startup communication.
- **`IRemoteModelListener`** — Observer for model state changes.
- **`IObserverWaitingToJoin`** — Callback for observer join requests.
- **`PlayerDisconnectAction`** — Handles player disconnection scenarios.
- **`ServerConnectionProps`** — Connection configuration (host, port, name, password).
- **`messages/ModeratorMessage`**, **`messages/ModeratorPromoted`** — Moderator action messages.

#### Login & Authentication (`startup/login/`)

- **`ClientLogin`** — Client-side P2P authentication. Prompts for game password via Swing dialog, sends HMAC-SHA512 response to server challenge, includes engine version in handshake.
- **`ClientLoginValidator`** — Server-side password validation.
- **`HmacSha512Authenticator`** — Cryptographic authentication using HMAC-SHA512 challenge-response.
- **`AuthenticationException`** — Authentication failure.

#### Startup UI (`startup/ui/`)

- **`PlayerTypes`** — Enum of player types (Human, various AIs).
- **`InGameLobbyWatcher`** / **`InGameLobbyWatcherWrapper`** — Posts game status to the lobby while a game is running.
- **`panels/main/game/selector/`** — Game file selection: `GameSelectorModel` (the `GameFileSelector` and `GameSelectorPanel` UI live in `game-headed`).
- **`FileBackedGamePropertiesCache`** / **`IGamePropertiesCache`** — Persists game property selections.

#### Startup Utilities

- **`LobbyWatcherThread`** / **`WatcherThreadMessaging`** — Background thread and messaging for lobby status updates.
- **`SystemPropertyReader`** — JVM property parser.

### Map Management Subsystem (~19 files)

The `map/` package handles map installation, discovery, and downloading.

#### Installed Maps

- **`InstalledMap`** — Represents one installed map. Wraps `MapDescriptionYaml` metadata. Key operations: `findContentRoot()` (locates `polygons.txt` directory), `getGameXmlFilePath(gameName)`, `readGameNotes(gameName)`, `isOutOfDate(download)`, `findMapSkin(name)` / `getSkinNames()`.
- **`InstalledMapsListing`** — Aggregates all installed maps. Scans the user maps folder, provides sorted game lists, case-insensitive map lookup, game XML path resolution, and identifies out-of-date maps.
- **`file/system/loader/`** — File system access for map loading (3 files).
- **`listing/MapListingFetcher`** — Retrieves available maps from the server.
- **`ZippedMapsExtractor`** — Extracts downloaded map ZIP files, auto-generates `map.yml` if missing.

#### Map Download System (`map/download/`, 9 files; the download window is in game-headed)

- **`DownloadCoordinator`** — Singleton managing concurrent downloads. Queue supports max 3 concurrent downloads. Methods: `accept(MapDownloadItem)`, `cancelDownloads()`, `pollTerminatedDownloads()`.
- **`DownloadFile`** — Single download state machine (NOT_STARTED → DOWNLOADING → DONE/CANCELLED). Downloads to temp, extracts, validates, generates `map.yml` if missing, flattens nested folder structure.
- **`ContentReader`** — HTTP file fetcher.
- **`DownloadFileParser`** / **`DownloadFileDescription`** — Download metadata parsing and container.
- **`FileSizeWatcher`** — Reports download progress by polling the file size.
- **`DownloadConfiguration`** / **`DownloadListener`** / **`ManagedMap`** — Configuration, callbacks, and the installed/available map pairing.

### Other Subsystems

- **`ui/DefaultGameChooserEntry`** — Game list entry (the `GameChooser` dialog is in game-headed).
- **`save/game/GameDataWriter`** — Writes game data to file.
- **`message/PlayerListing`** — Serializable snapshot of player status: player-to-node mappings, enabled/disabled states, type assignments, game name, round number, alliance order.
- **`system/HttpProxy`** — HTTP proxy configuration.
- **`lookandfeel/RelationshipColorScheme`** — Relationship colors (the look-and-feel setup is in game-headed).

### Game Startup Flows

**Local game:**
```
LocalLauncher.launch()   (game-headed)
  → GameDataManager loads game data
  → ServerGame created with LocalNoOpMessenger
  → IGameLoader.startGame() begins delegate execution
  → Delegates run on background thread
```

**Network server:**
```
ServerModel → ServerLauncher created
  → GameRelayServer started on port 6000
  → ServerMessenger listens for clients
  → ServerGame created when all clients ready
  → IGameStepAdvancer registered for remote step control
  → CryptoRandomSource provides verified random numbers
```

**Network client:**
```
ClientModel.connect()
  → ClientLogin authenticates (HMAC-SHA512)
  → ClientMessenger joins server
  → GameData received from server
  → ClientGame created, listens on IGameModifiedChannel
  → Server broadcasts state changes to all clients
```

### State Synchronization

All game state mutations flow through `Change` objects:
1. Server delegate calls `bridge.addChange(change)`
2. `ServerGame` applies change to local `GameData`
3. `IGameModifiedChannel` broadcasts `gameDataChanged(change)` to all clients
4. Each `ClientGame` applies the change to its local `GameData` copy
5. `HistorySynchronizer` (game-headed) maintains a separate history-browsable copy

## UI Layer

The Swing UI (map panel, drawables, image factories, gameplay panels, history and statistics views, help menus) lives in game-headed; see its `AGENTS.md`. The `ui/` package here keeps only Swing-free pieces that engine code or game-headless also use.

game-core does not depend on `swing-lib`, and `./verify` (`.build/code-convention-checks`) fails on any `javax.swing` import in its main code except `javax.swing.tree`, which `History` and `StatisticsAggregator` build on.

### Map Data (`mapdata/`, 3 files)

- **`MapData`** — Central repository for territory geometry and rendering properties (polygons, colors, placement coordinates, scroll wrapping, island detection).
- **`IslandTerritoryFinder`** — Identifies island territories.
- **`PlayerColors`** — Player color management.

### Other Subpackages

- **`display/`** (1 file) — `HeadlessDisplay`: stub `IDisplay` implementation for headless/bot mode.
- **`menubar/debug/`** (2 files) — `AiPlayerDebugAction` (interface; the `MapPanel`-backed implementation is in game-headed), `AiPlayerDebugOption`.
- **`panel/move/`** (1 file) — `MovableUnitsFilter` (shared with game-headed's `MovePanel`).
- **Root `ui/`** — `AdditionalTerritoryDetails`, `ObjectiveDummyDelegateBridge`, `PlaceData`, `OrderedProperties`; localized text in `NotificationMessages`, `PoliticsText`, `UserActionText`.
