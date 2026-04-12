# game-app/smoke-testing

End-to-end smoke tests that verify game engine functionality by parsing map XMLs,
loading/saving games, and running all-AI games to completion.

## Module Structure

All source is under `src/test/java/games/strategy/engine/data/`. There is no main
source — this module is tests only.

| File | Purpose |
|------|---------|
| `ParseGameXmlsTest` | Parameterized test that parses every XML in `src/test/resources/map-xmls/` (~95 maps) via `GameParser.parse()` |
| `GameSaveCompatibilityTest` | Downloads `.tsvg` save files (listed in `save-game-list.txt`) and deserializes them, asserting all major `GameData` accessors return non-null |
| `GameSaveTest` | Starts all-AI games on selected maps and writes a save file, verifying it is non-empty |
| `AiGameTest` | Runs all-AI games (Pro AI) to completion on `Test1.xml` and `imperialism_1974_board_game.xml`, asserting victory conditions and unit counts |
| `GameTestUtils` | Shared setup: configures headless mode, creates `ServerGame` with all Pro AI players, provides helpers for running game steps and counting units |
| `TestDataFileLister` | Lists files from a classpath directory for parameterized test providers |

## Build and Test Data

The Gradle build (`build.gradle`) uses the `de.undercouch.download` plugin to fetch
save game files before tests run:

- URLs are listed in `save-game-list.txt` (lines starting with `#` are ignored)
- Files download to `build/downloads/save-games/` and are copied into test resources
  as `save-games/`
- The `processTestResources` task depends on `downloadSaveGames`
- `maxHeapSize = "2G"` is set because `AiGameTest` is memory-intensive (concurrent
  battle calculator threads deserialize `GameData` concurrently)

## Test Patterns

**Headless setup** — `GameTestUtils.setUp()` must be called in `@BeforeAll`:
- Sets `TRIPLEA_HEADLESS=true`, creates a temporary `.triplea-root` directory
- Uses `MemoryPreferences` to avoid touching real user settings
- Disables AI move/combat pauses for fast execution
- Calls `HeadlessLaunchAction.setSkipMapResourceLoading(true)`

**Creating a game** — `GameTestUtils.setUpGameWithAis(xmlName)`:
- Parses the XML, assigns `PlayerTypes.PRO_AI` to all players
- Creates a `ServerGame` with `LocalNoOpMessenger` (no network)
- Disables delegate autosaves
- Returns a game ready for `runNextStep()` or `saveGame()`

**Step-by-step execution** — `GameTestUtils.runStepsUntil(game, stepName)` runs game
steps until the named step is reached, used by AI tests that need to inspect state at
specific phases.

## Key Dependencies

- `game-app:game-core` — `GameData`, `GameParser`, `ServerGame`, delegates
- `game-app:game-headless` — `HeadlessGameServer`, `HeadlessLaunchAction`
- `game-app:domain-data` — domain model types
- `lib:test-common` — shared test utilities
