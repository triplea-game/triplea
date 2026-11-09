# game-headed

The desktop GUI client for TripleA. This is the main user-facing application — a Swing-based client that provides the full game experience including lobby, game setup, gameplay UI, and battle calculator.

## Entry Point

**Main class**: `org.triplea.game.client.HeadedGameRunner`

The `main()` method initializes logging, look-and-feel, client settings, and desktop integrations (macOS URI handlers, file associations), then launches the game setup UI.

### CLI Properties / Startup Modes

Startup behavior is controlled by system properties (set via CLI arguments):

- **`triplea.game`** — Load a save file or game map XML directly
- **`triplea.server`** — Start as a network game server
- **`triplea.client`** — Start as a network game client
- **`triplea.map.download`** — Open the map download window for a specific map name
- **`triplea.start`** — Startup mode selector. Values: `"lobby"` (lobby login), `"local"` (single-player), `"pbem"` (Play-By-Email), `"pbf"` (Play-By-Forum)

macOS-specific: opening a file or a `triplea://` URI is translated to the corresponding property.

## Package Structure

- **`org.triplea.game.client`** — Client bootstrap: `HeadedGameRunner` (entry point), `HeadedApplicationContext` (provides main class reference for `ApplicationContext`)
- **`org.triplea.game.client.ui.swing.laf`** — Swing L&F setup (`SubstanceLookAndFeelManager`, `DefaultSubstanceLookAndFeelManager`)
- **`org.triplea.sound`** — Audio playback (`DefaultSoundChannel` — routes server-side sound events to local `ClipPlayer` based on which players are local; `SoundOptions` menu items)
- **`org.triplea.lobby.common`** — `LobbyGameUpdateListener` interface (notifies of game additions/removals via WebSocket)
- **`games.strategy.engine.data.properties`** — Swing editors for game properties: `PropertyEditorFactory` (one editor per core property type), `PropertiesUi` (panel of labelled editors)
- **`games.strategy.engine.chat`** — Chat Swing UI: `ChatPanel`, `ChatMessagePanel`, `ChatPlayerPanel`, `PlayerChatRenderer`
- **`games.strategy.engine.framework.ui`** — `MainFrame` — main game frame and window management; `GameChooser`, `GameNotesView`; `background/` wait dialogs and `BackgroundTaskRunner` for long operations off the EDT
- **`games.strategy.engine.framework.lookandfeel`** — `LookAndFeel`, `LookAndFeelSwingFrameListener` — UI theme management
- **`games.strategy.engine.framework.startup.launcher`** — `LocalLauncher` (single-computer game launch)
- **`games.strategy.engine.framework.startup.ui`** — Game setup panels (local, server, client, meta-setup, player selector)
- **`games.strategy.engine.framework.startup.ui.panels.main`** — `MainPanel`/`MainPanelBuilder`, `HeadedServerSetupModel`
- **`games.strategy.engine.framework.startup.ui.panels.main.game.selector`** — `GameSelectorPanel` (game/save selection and game options dialog)
- **`games.strategy.engine.framework.network.ui`** — Host management actions: `ChangeGameOptionsClientAction` (edit a host bot's game options), `SetMapClientAction`, `SetPasswordAction`, `ChangeGameToSaveGameClientAction`, `BanPlayerAction`/`BootPlayerAction`
- **`games.strategy.engine.framework.startup.ui.posted.game`** — PBEM/PBF setup panels (`PbemSetupPanel`, `PbfSetupPanel`) and editors (`DiceServerEditor`, `EmailSenderEditor`, `ForumPosterEditor` with their view models)
- **`games.strategy.engine.framework.startup.mc`** — `HeadedLaunchAction` (creates `TripleAFrame` and `TripleADisplay` at game start), `HeadedPlayerTypes` (maps player type enums to implementations — human, client, DoesNothing, FlowFieldAi), `HeadedServerStartupRemote`
- **`games.strategy.engine.auto.update`** — Startup update checks: engine version (every 2 days via `EngineVersionCheck`), tutorial map presence (`TutorialMapCheck`), outdated downloaded maps (`UpdatedMapsCheck`). Orchestrated by `UpdateChecks`.
- **`games.strategy.engine.framework`** — `GameProcess` (external process launching), `ProcessRunnerUtil`, `HistorySynchronizer` (keeps a history-browsable `GameData` copy for the history panel)
- **`games.strategy.engine.framework.map.download`** — `DownloadMapsWindow` (tabs for available/installed/out-of-date maps) with its model, table and progress panel; `MapDownloadController` — controls tutorial map download prompting (checks preference + user map folder). The download engine (`DownloadCoordinator`, `DownloadFile`) is in game-core
- **`games.strategy.engine.lobby.client.login`** — Login flow (8 files, see [Login & Registration](#login--registration-lobbyclientlogin-8-files))
- **`games.strategy.engine.lobby.client.ui`** — Lobby UI: `LobbyFrame`, `LobbyGamePanel`, `LobbyGameTableModel`
- **`games.strategy.engine.lobby.client.ui.action`** — Player context-menu actions: `ShowPlayersAction`, moderator actions (`BanPlayerModeratorAction`, `DisconnectPlayerModeratorAction`, `MutePlayerAction`), `ActionConfirmation`/`ActionDuration`/`ActionDurationDialog` helpers
- **`games.strategy.engine.lobby.client.ui.action.player.info`** — Player information popup with tabs: `PlayerAliasesTab`, `PlayerBansTab`, `PlayerGamesTab`, `PlayerInfoSummaryTextArea`
- **`games.strategy.engine.lobby.moderator.toolbox`** — Moderator administration tools (see [Moderator Toolbox](#moderator-toolbox-lobbymoderatortoolbox-21-files))
- **`games.strategy.engine.posted.game`** — PBEM/PBF turn-posting panels: `AbstractForumPosterPanel` (extends `ActionPanel`), `EndTurnPanel`, `MoveForumPosterPanel`, `ForumPosterComponent`
- **`games.strategy.triplea`** — `TripleAPlayer` (human player controller — extends `AbstractBasePlayer`, manages all turn phases by delegating to `TripleAFrame` UI methods)
- **`games.strategy.triplea.odds.calculator`** — `BattleCalculatorDialog` (battle outcome odds calculator, extends `JDialog`) and its panels
- **`games.strategy.triplea.image`** — Image factories for units, flags, dice, resources, PUs, territory effects and map tiles
- **`games.strategy.triplea.printgenerator`** — `SetupFrame` (export setup charts) with `PuChart`, `StatsInfo`
- **`games.strategy.ui`** — Image scroller views (`ImageScrollerLargeView`, `ImageScrollerSmallView`), `ScrollableTextField`
- **`games.strategy.triplea.ui`** — Gameplay UI components (see [Gameplay UI](#gameplay-ui))
- **`games.strategy.triplea.ui.menubar`** — Main menu bar (File, View, Game, Export, Network, Lobby, Debug menus — 8 files)
- **`games.strategy.triplea.ui.display`** — `TripleADisplay` (implements `IDisplay`, routes engine display events to `TripleAFrame` methods)
- **`games.strategy.triplea.ui.export`** — `ScreenshotExporter` (renders map to PNG with optional title overlay)
- **`games.strategy.triplea.ui.panel.move`** — `MovePanel` (movement phase UI), `DoneMoveAction`
- **`tools.map.making.ui`**, **`tools.util`** — Map-making tools (`MapCreator`, launched from `MetaSetupPanel`) and their file/property helpers

## Running

```bash
./gradlew :game-headed:run
```

Game assets are downloaded to `build/downloads/assets-zip` by the `downloadAssets` task, unzipped to `build/assets`, and bundled as classpath resources.

## Gameplay UI

The headed module provides ~20 gameplay UI files in `games.strategy.triplea.ui` plus subpackages for menus, display, export, and movement.

### TripleAFrame — Main Game Window

`TripleAFrame` (extends `JFrame`, implements `QuitHandler`) is the main game window. Layout:

- **Map area**: `MapPanel` (interactive game map) + `ImageScrollerSmallView` (minimap in NORTH position)
- **Tabbed pane** (`JTabbedPane`) with keyboard shortcuts:
  - "Actions" (Ctrl+C) — `ActionButtonsPanel` (phase-specific action panels)
  - "Players" (Ctrl+P) — `StatPanel` (player statistics)
  - "Technology" (Ctrl+Y) — `TechnologyPanel` (conditional: only if tech is in the game)
  - "Resources" (Ctrl+R) — `EconomyPanel`
  - Custom objective tab (Ctrl+O) — `ObjectivePanel` (conditional: only if objectives exist in game data)
  - "Territory" (Ctrl+T) — `TerritoryDetailPanel`
- **Conditional panels**: `EditPanel` (shown as tab when edit mode is active), `HistoryPanel` (game history navigation)
- **Other components**: `ChatPanel` (multiplayer chat, nullable), `CommentPanel`, `BottomBar`
- **Menu bar**: `TripleAMenuBar`

### ActionPanel Hierarchy

`ActionPanel` (abstract, extends `JPanel`) is the base class for all turn-phase action panels. Each panel controls one phase of a player's turn, using `waitForRelease()`/`release()` with `CountDownLatch` for thread synchronization between the game engine and the UI.

```
ActionPanel (abstract base)
├── BattlePanel          — Battle display, casualty selection, retreat decisions
├── PurchasePanel        — Unit purchasing phase
├── TechPanel            — Technology research selection
├── RepairPanel          — Unit repair
├── PoliticsPanel        — Political actions and relationship changes
├── UserActionPanel      — Custom user-defined actions
├── EditPanel            — Admin game editing (add/remove units, change ownership, etc.)
├── PickTerritoryAndUnitsPanel — Territory and unit picker
├── AbstractMovePanel (abstract)
│   ├── MovePanel        — Movement phase (in ui/panel/move/)
│   └── PlacePanel       — Unit placement phase
└── AbstractForumPosterPanel (abstract, in engine/posted/game/)
    ├── EndTurnPanel         — End-of-turn summary posting
    └── MoveForumPosterPanel — Move summary posting
```

### Undo Support

- **`AbstractUndoableMovesPanel`** — Base panel for displaying undoable actions
  - **`UndoableMovesPanel`** — Undo support for movement
  - **`UndoablePlacementsPanel`** — Undo support for placements

### ActionButtonsPanel

Manages the set of `ActionPanel` instances using `CardLayout`, showing the appropriate panel for the current game phase. Holds references to: `BattlePanel`, `MovePanel`, `PurchasePanel`, `RepairPanel`, `PlacePanel`, `TechPanel`, `EndTurnPanel`, `MoveForumPosterPanel`, `PoliticsPanel`, `UserActionPanel`, `PickTerritoryAndUnitsPanel`.

### Other Gameplay UI

- **`CommentPanel`** — Game comment display/entry.
- **`FindTerritoryDialog`** / **`FindTerritoryAction`** — Search for territories by name.
- **`TerritoryDetailPanel`** — Displays detailed territory information (units, production, owner).

### Menu Bar (`ui/menubar/`, 8 files)

- **`TripleAMenuBar`** — Main menu bar builder, assembles all submenus.
- **`FileMenu`** — Save, load, post PBEM/PBF turns.
- **`ViewMenu`** — Map display options (zoom, unit display flags, map skin selection).
- **`GameMenu`** — Game options and settings.
- **`ExportMenu`** — Export setup/stats/unit stats as text or images.
- **`NetworkMenu`** — Network game management (change map, password, boot/ban players).
- **`LobbyMenu`** — Lobby connection options.
- **`DebugMenu`** — Developer debug options.

Help menu content (`HelpMenu`, `MoveHelpMenu`, `UnitHelpMenu`, etc.) is in `ui/menubar/help/`; see [Help Menu](#help-menu-menubarhelp).

### Display Channel (`ui/display/`)

- **`TripleADisplay`** — Implements `IDisplay` interface. Routes engine display events (battle notifications, casualty selection, step changes) to `TripleAFrame` methods on the Swing EDT.

## UI Components

The Swing components behind the gameplay UI, in `games.strategy.triplea.ui` and its subpackages. Game-headless has no UI; it uses core's `HeadlessDisplay` instead.

### UiContext — Central UI State

`UiContext` is the master UI context object, created once per game session. It holds:
- Image factories (units, resources, dice, flags, territory effects, map images)
- `MapData` (territory geometry, rendering properties)
- Scale factor and display preferences
- Current player state
- Resource loader for the active map skin
- Window lifecycle management (`isShutDown()`)

Most UI components receive `UiContext` as a constructor parameter.

### Map Rendering System (`screen/`, `screen/drawable/`)

The map is rendered using a **tile-based, layered architecture**.

**Tile System** (5 files in `screen/`):
- **`TileManager`** — Orchestrates tile rendering. Determines which tiles need redrawing, collects `IDrawable` objects for each tile, and composites them in layer order.
- **`Tile`** — Individual 256×256 pixel map tile with bounds and cached image.
- **`UnitsDrawer`** — Renders unit stacks on tiles (flag icons, unit counts, damage markers).
- **`SmallMapImageManager`** — Generates and caches the minimap image.
- **`TerritoryOverLayDrawable`** — Territory highlighting overlays (selection, mouseover).

**Layer System** (17 files in `screen/drawable/`):
All drawables implement `IDrawable`, which defines a `DrawLevel` enum controlling render order (ascending ordinal):

```
DrawLevel (render order, bottom to top):
  BASE_MAP_LEVEL          — BaseMapDrawable (map background image)
  POLYGONS_LEVEL          — LandTerritoryDrawable, SeaZoneOutlineDrawable (territory fills, sea borders)
  RELIEF_LEVEL            — ReliefMapDrawable (3D relief shading)
  TERRITORY_EFFECT_LEVEL  — TerritoryEffectDrawable (special territory effects)
  CAPITOL_MARKER_LEVEL    — CapitolMarkerDrawable, BlockadeZoneDrawable, ConvoyZoneDrawable, KamikazeZoneDrawable
  VC_MARKER_LEVEL         — VcDrawable (victory condition markers)
  DECORATOR_LEVEL         — DecoratorDrawable (composite wrapper)
  TERRITORY_TEXT_LEVEL    — TerritoryNameDrawable (territory names and PU values)
  BATTLE_HIGHLIGHT_LEVEL  — BattleDrawable (battle location highlights)
  UNITS_LEVEL             — (unit stacks, rendered by UnitsDrawer)
  TERRITORY_OVERLAY_LEVEL — (selection/hover highlights)
```

Other drawables: `AbstractDrawable` (base impl), `TerritoryDrawable` (base territory rendering), `MapTileDrawable` (individual tile rendering).

### Map Panel (`panels/map/`, 6 files)

- **`MapPanel`** — The main interactive map display (extends `ImageScrollerLargeView`). Manages `TileManager`, handles pan/zoom, unit selection, territory highlighting, and route drawing.
- **`MapSelectionListener`** — Territory click/hover events.
- **`UnitSelectionListener`** — Unit click events.
- **`MouseOverUnitListener`** — Unit hover detection.
- **`MapRouteDrawer`** — Draws movement routes on the map.
- **`RouteDescription`** — Route path data.

### Root UI Components (`ui/`)

Key classes by functional area:

**Production & Resources:**
- `ProductionPanel`, `EditProductionPanel`, `TabbedProductionPanel` — Unit purchase UI
- `ProductionRepairPanel` — Unit repair
- `ProductionTabsProperties` — Tab configuration
- `ResourceBar`, `ResourceChooser` — Resource display and selection

**Battle & Combat Display:**
- `BattleDisplay`, `BattleModel`, `BattleStepsPanel` — Battle visualization
- `CasualtySelection` — Casualty selection UI
- `DicePanel`, `DiceChooser` — Dice display and selection

**Player & Territory Info:**
- `StatPanel`, `ExtendedStats` — Player statistics tables
- `EconomyPanel`, `TechnologyPanel` — Economic and tech displays
- `PlayersPanel` — Player list
- `ObjectivePanel`, `ObjectiveProperties` — Victory objectives
- `TerritoryDetailPanel` — Territory information
- `PoliticalStateOverview` — Alliance/war status
- `SelectTerritoryComponent` — Territory picker

**Unit Display:**
- `SimpleUnitPanel`, `IndividualUnitPanel`, `IndividualUnitPanelGrouped` — Unit rendering panels
- `UnitChooser`, `PlayerChooser` — Selection dialogs
- `PlacementUnitsCollapsiblePanel` — Placement support
- `MapUnitTooltipManager`, `TooltipProperties`, `UnitIconProperties` — Tooltip and icon configuration

**Utility:**
- `UiContext` — Master UI context (see above)
- `MacOsIntegration`, `QuitHandler` — Platform integration
- `MouseDetails` — Mouse event info
- `BottomBar` — Status bar
- `FlagDrawMode` — Flag rendering mode
- `VerifiedRandomNumbersDialog` — Verified dice display

### Unit Scroller (`unit/scroller/`, 5 files)

Navigation widget that helps players "scroll through" units that haven't moved yet:
- **`UnitScroller`** — Previous/Next buttons, territory icon, skip/sleep functionality
- **`UnitScrollerModel`** — Tracks unmoved units across combat and non-combat phases
- **`UnitScrollerIcon`**, **`AvatarPanelFactory`**, **`AvatarCoordinateCalculator`** — Icon rendering

### History (`history/`, 3 files)

- **`HistoryPanel`** — JTree-based UI showing game history (moves, battles, events). Supports expand/collapse, right-click context menu, and integration with `HistoryDetailsPanel`.
- **`HistoryLog`** — Detailed text view dialog for history nodes. Formats moves, battles, dice rolls, and resource changes. Supports verbose logging toggle.
- **`HistoryDetailsPanel`** — Displays details for the selected history node.

### Statistics (`statistics/`, 1 file)

- **`StatisticsDialog`** — Uses XChart library to display player statistics over time in tabbed graphs.

### Logic (`logic/`, 2 files)

- `MapScrollUtil`, `RouteCalculator` — Map scrolling and wrapped-map route geometry.

### Help Menu (`menubar/help/`)

`HelpMenu` (builder), `MoveHelpMenu`, `UnitHelpMenu`, `GameNotesMenu`, `UnitStatsTable`, `InformationDialog`.

The AI debug menu's `AiPlayerDebugAction` interface and `AiPlayerDebugOption` stay in game-core `ui/menubar/debug/`, so the AI can contribute menu entries.

## Lobby UI

The lobby system (~24 files across `games.strategy.engine.lobby`) provides:

### Login & Registration (`lobby/client/login/`, 8 files)

- **`LobbyLogin`** — Orchestrates the full login flow (login, account creation, password recovery, forced password change)
- **`LoginPanel`** — Login dialog with username/password fields, "Login Without An Account" (anonymous) checkbox, "Remember Password" option
- **`CreateAccountPanel`** — New account registration (username, email, password with confirmation)
- **`ForgotPasswordPanel`** — Password recovery (username + email)
- **`ChangePasswordPanel`** — Forced password change after login with a temporary password
- **`ChangeEmailPanel`** — Email update dialog
- **`LoginMode`** — Enum: `REGISTRATION_REQUIRED` / `REGISTRATION_NOT_REQUIRED` (controls anonymous login availability)
- **`LoginResult`** — Login response data: apiKey, username, anonymousLogin flag, moderator flag, passwordChangeRequired flag, optional loginMessage

### Game Browser (`lobby/client/ui/`, 3 files)

- **`LobbyFrame`** — Top-level lobby window with chat system (`ChatMessagePanel` + `ChatPlayerPanel`), game listing, and player context menus
- **`LobbyGamePanel`** — Game listing `JTable` with sorting, filtering, and toolbar ("Host Game" / "Join Game" buttons). Right-click context menu: Join Game, Host Game, Show Players, Boot Game (moderator), Shutdown (moderator). Bot games shown in italic. Double-click to join.
- **`LobbyGameTableModel`** — 9 table columns (Host, Name, Round, Players, Password indicator, Status, Comments, Started, UUID). Real-time updates via WebSocket. Admin-only columns hidden for non-moderators.

### Player Actions (`lobby/client/ui/action/`, 7 files + `player/info/`, 6 files)

Right-click actions on players in the lobby chat panel:
- **All users**: "Show Player Info" (`ShowPlayerInformationAction` — tabbed popup with aliases, bans, games history, summary)
- **Moderators only**: "Mute Player" (`MutePlayerAction`), "Disconnect Player" (`DisconnectPlayerModeratorAction`), "Ban Player" (`BanPlayerModeratorAction`)
- **Helper classes**: `ActionConfirmation`, `ActionDuration`, `ActionDurationDialog`, `ShowPlayersAction`

### Moderator Toolbox (`lobby/moderator/toolbox/`, 21 files)

`ToolBoxWindow` opens a `JDialog` with tabbed administration panels (created by `TabFactory`):

| Tab | Package | Description |
|-----|---------|-------------|
| Access Log | `tabs/access/log/` | View player access logs, with ban action |
| Bad Words | `tabs/bad/words/` | Manage chat filter word list (add/remove) |
| Banned Names | `tabs/banned/names/` | Manage banned username patterns |
| Banned Users | `tabs/banned/users/` | View/manage user bans |
| Event Log | `tabs/event/log/` | View moderator action audit log |
| Maps | `tabs/maps/` | Manage game maps listing |
| Moderators | `tabs/moderators/` | Manage moderator accounts (add/remove, show API keys) |

Each tab follows a consistent MVC pattern: `*Tab` (view), `*TabModel` (data), `*TabActions` (user interactions). Common utilities: `Pager` (pagination), `ShowApiKeyDialog`, `MessagePopup`.

## Game Setup UI

Setup panels in `games.strategy.engine.framework.startup.ui/`:
- **`MetaSetupPanel`** — Top-level setup panel (choose play mode)
- **`LocalSetupPanel`** — Single-player game setup
- **`ServerSetupPanel`** — Host a network game
- **`ClientSetupPanel`** — Join a network game
- **`SetupPanel`** — Abstract base for setup panels
- **`PlayerSelectorRow`** — Player type selection (Human, AI variant)
- **`MainPanel`** / **`MainPanelBuilder`** — Main setup window container
- **`HeadedServerSetupModel`** — Server setup state management

PBEM/PBF setup in `startup/ui/posted/game/`:
- **`PbemSetupPanel`** — Play-by-email configuration
- **`PbfSetupPanel`** — Play-by-forum configuration
