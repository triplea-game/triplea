# docs/ index

One line per doc so you can pick the right file without opening several. Size in lines.
Read this index first; open a doc only when its line says it answers the question. For the two
large tutorials, jump to a section by its anchor (`grep -n 'sec_5.3.10' <file>`, then Read with
offset) instead of reading the whole file.

Markers: **[stale]** may be outdated, verify against the code; **[not here]** describes code or
servers outside this repository; **[human]** process for people, rarely needed for coding.

## development/ (for code changes)

| Doc | Lines | What it answers |
|---|---|---|
| `development/README.md` | 186 | Dev setup: JDK 25, IDE, Mac/Windows notes, git ignore-revs, compile and launch from CLI. [human] |
| `development/windows-dev-setup.md` | 236 | Native Windows setup, IDE-neutral: tools, JAVA_HOME check, clone, run, log locations and levels, debugging via `--debug-jvm` + attach (IntelliJ, VS Code, Eclipse, jdb), troubleshooting. [human] |
| `development/build-overview-and-development.md` | 100 | Gradle structure and the convention plugins in `gradle/build-logic` (test-conventions, base-project, java-library, java-application). |
| `development/engine-code-overview.md` | 197 | Original engine design: GameData, game XML contents, delegates, players, changes, bridges. [stale] |
| `development/map-loading-design.md` | 26 | XML -> game objects in two steps: `xml-reader` parses to POJOs in `map-data`, then assembly adds meaning. |
| `development/glossary.md` | 33 | Domain terms: game core (Node, PlayerName, ...), map XML terms, battle terms (round, first strike, AA). |
| `development/game-rule-sets.md` | 18 | Table of rule variants (V1-V6, LHTR, ...) with example game XMLs and rule books. |
| `development/error-handling-and-logging.md` | 29 | Log levels map to UI: severe = crash dialog with report button, warning = dialog, info = console only; messages are for players. |
| `development/ai-overview-and-backlog.md` | 87 | AI feature backlog, then how the AI plans combat move, non-combat move, purchase, place; code organisation. |
| `development/i18n_languages.md` | 48 | State of internationalisation and FAQ on adding translated strings. |
| `development/game-asset-management.md` | 16 | Sounds/images come from the separate `assets` repo into `game-headed/assets`; why. |
| `development/versioning.md` | 53 | Product version (two digits, eg 2.6) vs build version, when each is incremented. |
| `development/lobby-authentication-logic.md` | 27 | Lobby login flow: password hashing, api-key, player-chat-id. Server side is [not here]. |
| `development/maps-server.md` | 19 | How maps are uploaded (GitHub triplea-maps repos + `map.yml`) and listed/downloaded. Server is [not here]. |
| `development/database-unit-test.md` | 57 | DB Rider/DBUnit tests for the lobby database. [not here] |
| `development/db-migration-file-versioning.md` | 21 | Naming of Flyway-style SQL migration files. [not here] |
| `development/how-to/make-database-changes.md` | 24 | Adding DB migration files and testing them. [not here] |
| `development/how-to/performance-profiling-triplea.md` | 29 | Profiling TripleA, eg to speed up AI turns. |
| `development/how-to/debugging-memory-leaks.md` | 17 | Heap dumps with VisualVM. |
| `development/how-to/qa-testing.md` | 45 | Manual regression test list, incl. network games old/new client compatibility. [human] |
| `development/typical-git-workflow.md` | 41 | Fork/upstream git commands for contributors. [human] |
| `development/ide-setup/intellij-setup.md` | 94 | IntelliJ plugins and settings, google-java-format fix. [human] |
| `development/ide-setup/eclipse.md` | 57 | Eclipse formatter/cleanup/import order. [human] [stale] |
| `development/initiatives-and-tech-debt/development-initiatives.md` | 47 | Long-term goals: code re-organisation, community, AI, maps, gameplay. |
| `development/initiatives-and-tech-debt/locations-using-reflection.md` | 32 | Where reflection is still used (issue #3865), for the effort to remove it. |
| `development/ui/CasualtySelection.puml`, `UnitChooser.puml` | 73, 59 | PlantUML diagrams of the casualty selection and unit chooser UI. |

## development/code-conventions/

Already turned into requirements in `.agents/rules/` (`java.md`, `tests.md`, `shell.md`). Open
the source only for an example the rule does not show.

| Doc | Lines | What it answers |
|---|---|---|
| `java-code-conventions.md` | 385 | Every Java convention with good/avoid examples. |
| `java-test-code-conventions.md` | 43 | `@DisplayName`, AssertJ with `.as()`, given/when/then helpers. |
| `naming-conventions.md` | 38 | `newFoo()` factories, kebab-case URLs, DB snake_case naming. |
| `shell-script-conventions.md` | 44 | Bash script style. |
| `database.md` | 9 | Use timestamptz in UTC. [not here] |

## map-making/ (map XML, attachments, map.yml)

| Doc | Lines | What it answers |
|---|---|---|
| `map-making/tutorial/creating-custom-map-xml.md` | 1370 | Game XML reference by section: `sec_5.2.1` map properties, `5.2.2` capitals, `5.2.3` victory cities, `5.3.1` game header, `5.3.2` territories, `5.3.3` connections, `5.3.4` resources, `5.3.5` players and alliances, `5.3.6` units, `5.3.7` delegates, `5.3.8` sequence and steps, `5.3.9` production rules, `5.3.10` unit attachment, `5.3.11` tech attachment. |
| `map-making/tutorial/map-and-map-skin-making.md` | 916 | Map images and skins: `sec_2` map creator, `sec_3` border image, `sec_4.1.x` utilities (properties maker, center picker, polygon grabber, placement, tiles, decorations, relief, shrinker), `sec_5` other map folders and files. |
| `map-making/reference/map-yml-file.md` | 83 | `map.yml`: location, contents, how the engine and maps server use it. |
| `map-making/reference/map-features-change-log.md` | 79 | Map XML / map.properties / sound changes by version; add new map features here. |
| `map-making/reference/game-notes.md` | 18 | Game notes HTML file next to the game XML: naming and contents. |
| `map-making/how-to/updating-existing-maps.md` | 16 | Changing an existing map vs creating a mod. [human] |
| `map-making/how-to/uploading-a-map-to-triplea.md` | 77 | Publishing a map via GitHub. [human] |

## Project, admin, infrastructure (rarely needed for code)

| Doc | Lines | What it answers |
|---|---|---|
| `README.md` | 32 | Docs audiences and types. |
| `contribute.md` | 206 | Contribution roles (player, tester, map maker, developer, ...). [human] |
| `project/pull-requests.md` | 55 | PR workflow and tips (self-review, commit message). |
| `admin/release-steps.md` | 54 | Release branch, version bump, release notes, `servers.yml`, hotfix. [human] |
| `admin/how-to-upgrade-gradle.md` | 7 | `./gradlew wrapper --gradle-version=<v>`. |
| `admin/upgrading-install4j.md` | 25 | Installer file, license key, install4j config. [human] |
| `admin/map-maintenance.md` | 12 | Map admin chores. [human] |
| `admin/cleaning-up-tags-and-releases.md` | 9 | Deleting tags and releases. [human] |
| `admin/todo-old-download-links` | 5 | Old download sites to update (issue #6461). |
| `infrastructure/*` (7 files) | 13-99 | Ansible, secrets, server setup, lobby and bot ops. [not here] [human] |
