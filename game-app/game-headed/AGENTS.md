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
- **`org.triplea.game.client.ui.swing.laf`** — Swing L&F setup (`DefaultSubstanceLookAndFeelManager`)
- **`org.triplea.sound`** — Audio playback (`DefaultSoundChannel` — routes server-side sound events to local `ClipPlayer` based on which players are local)
- **`org.triplea.lobby.common`** — `LobbyGameUpdateListener` interface (notifies of game additions/removals via WebSocket)
- **`games.strategy.engine.framework.ui`** — `MainFrame` — main game frame and window management
- **`games.strategy.engine.framework.startup.ui`** — Game setup panels (local, server, client, meta-setup, player selector)
- **`games.strategy.engine.framework.startup.ui.panels.main`** — `MainPanel`/`MainPanelBuilder`, `HeadedServerSetupModel`
- **`games.strategy.engine.framework.startup.ui.posted.game`** — PBEM/PBF setup panels (`PbemSetupPanel`, `PbfSetupPanel`)
- **`games.strategy.engine.framework.startup.mc`** — `HeadedLaunchAction` (creates `TripleAFrame` and `TripleADisplay` at game start), `HeadedPlayerTypes` (maps player type enums to implementations — human, client, DoesNothing, FlowFieldAi), `HeadedServerStartupRemote`
- **`games.strategy.engine.auto.update`** — Startup update checks: engine version (every 2 days via `EngineVersionCheck`), tutorial map presence (`TutorialMapCheck`), outdated downloaded maps (`UpdatedMapsCheck`). Orchestrated by `UpdateChecks`.
- **`games.strategy.engine.framework`** — `GameProcess` (external process launching), `ProcessRunnerUtil`
- **`games.strategy.engine.framework.map.download`** — `MapDownloadController` — controls tutorial map download prompting (checks preference + user map folder)
- **`games.strategy.engine.lobby.client.login`** — Login flow (8 files, see [Login & Registration](#login--registration))
- **`games.strategy.engine.lobby.client.ui`** — Lobby UI: `LobbyFrame`, `LobbyGamePanel`, `LobbyGameTableModel`
- **`games.strategy.engine.lobby.client.ui.action`** — Player context-menu actions: `ShowPlayersAction`, moderator actions (`BanPlayerModeratorAction`, `DisconnectPlayerModeratorAction`, `MutePlayerAction`), `ActionConfirmation`/`ActionDuration`/`ActionDurationDialog` helpers
- **`games.strategy.engine.lobby.client.ui.action.player.info`** — Player information popup with tabs: `PlayerAliasesTab`, `PlayerBansTab`, `PlayerGamesTab`, `PlayerInfoSummaryTextArea`
- **`games.strategy.engine.lobby.moderator.toolbox`** — Moderator administration tools (see [Moderator Toolbox](#moderator-toolbox))
- **`games.strategy.engine.posted.game`** — PBEM/PBF turn-posting panels: `AbstractForumPosterPanel` (extends `ActionPanel`), `EndTurnPanel`, `MoveForumPosterPanel`, `ForumPosterComponent`
- **`games.strategy.triplea`** — `TripleAPlayer` (human player controller — extends `AbstractBasePlayer`, manages all turn phases by delegating to `TripleAFrame` UI methods)
- **`games.strategy.triplea.odds.calculator`** — `BattleCalculatorDialog` (battle outcome odds calculator, extends `JDialog`)
- **`games.strategy.triplea.ui`** — Gameplay UI components (see [Gameplay UI](#gameplay-ui))
- **`games.strategy.triplea.ui.menubar`** — Main menu bar (File, View, Game, Export, Network, Lobby, Debug menus — 8 files)
- **`games.strategy.triplea.ui.display`** — `TripleADisplay` (implements `IDisplay`, routes engine display events to `TripleAFrame` methods)
- **`games.strategy.triplea.ui.export`** — `ScreenshotExporter` (renders map to PNG with optional title overlay)
- **`games.strategy.triplea.ui.panel.move`** — `MovePanel` (movement phase UI), `DoneMoveAction`

## Dependencies

- `game-app:game-core` (core engine), `game-app:ai` (AI players), `game-app:domain-data`, `game-app:map-data`
- `http-clients:lobby-client` (lobby communication)
- `lib:swing-lib` (Swing utilities), `lib:java-extras`, `lib:feign-common`, `lib:websocket-client`

## Running

```bash
./gradlew :game-app:game-headed:run
```

Game assets are downloaded to `build/downloads/assets-zip` by the `downloadAssets` task, unzipped to `build/assets`, and bundled as classpath resources.

## Gameplay UI

The headed module provides ~20 gameplay UI files in `games.strategy.triplea.ui` plus subpackages for menus, display, export, and movement.

### TripleAFrame — Main Game Window

`TripleAFrame` (extends `JFrame`, implements `QuitHandler`) is the main game window. Layout:

- **Map area**: `MapPanel` (interactive game map from game-core) + `ImageScrollerSmallView` (minimap in NORTH position)
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

Note: Help menu content (`HelpMenu`, `MoveHelpMenu`, `UnitHelpMenu`, etc.) lives in **game-core** `ui/menubar/help/`.

### Display Channel (`ui/display/`)

- **`TripleADisplay`** — Implements `IDisplay` interface. Routes engine display events (battle notifications, casualty selection, step changes) to `TripleAFrame` methods on the Swing EDT.

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
