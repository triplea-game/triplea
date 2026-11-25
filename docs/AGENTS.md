# docs/

Project documentation organized by audience and type. These are standalone
reference documents — they are **not** auto-generated and may lag behind the
code.

## Directory Layout

| Path | Audience | Contents |
|------|----------|----------|
| `admin/` | Maintainers | Release steps, Gradle upgrades, install4j upgrades, map maintenance |
| `development/` | Developers | Build overview, code conventions, engine design, glossary, IDE setup, how-tos |
| `development/code-conventions/` | Developers | Java, test, database, naming, and shell-script style guides |
| `development/how-to/` | Developers | Debugging memory leaks, database changes, performance profiling, QA testing |
| `development/ide-setup/` | Developers | Eclipse and IntelliJ configuration |
| `development/initiatives-and-tech-debt/` | Developers | Development initiatives, locations using reflection |
| `development/ui/` | Developers | PlantUML diagrams (CasualtySelection, UnitChooser) |
| `infrastructure/` | Ops / Admins | Server setup, deployment design, lobby and bot ops, Ansible playbooks |
| `map-making/` | Map creators | Tutorials, how-tos, and reference for creating and uploading maps |
| `project/` | Contributors | Pull-request guidelines |

## Key Documents for AI Agents

| Document | Why it matters |
|----------|---------------|
| `development/engine-code-overview.md` | **Legacy** design doc — describes Delegates, GameData, GamePlayer, Changes, and Bridges. Referenced from root `AGENTS.md`. Useful for architectural concepts but details may be outdated. |
| `development/glossary.md` | Canonical definitions of domain terms (Node, PlayerId, Battle Round, First Strike, AA, etc.). Use this to resolve ambiguous terminology. |
| `development/code-conventions/java-code-conventions.md` | Full Java style guide — Google Java Style plus project-specific rules (Optional returns, no boolean params, step-down ordering, var usage, etc.). The root `AGENTS.md` summarizes these. |
| `development/code-conventions/java-test-code-conventions.md` | Test conventions — Hamcrest `assertThat` preferred, `@DisplayName` on tests, given/when/then structure. |
| `development/build-overview-and-development.md` | Build system overview — convention plugins, test fixtures, and future improvement plans. |
| `development/error-handling-and-logging.md` | Logging strategy — severe = crash dialog, warning = user-facing dialog, info = hidden console. |
| `development/lobby-authentication-logic.md` | Lobby auth flow details. |

## Staleness Notes

- `development/engine-code-overview.md` is explicitly labeled as *"the original
  design document … quite old, potentially out of date, left intact for
  historical reference."* Cross-check against actual code before relying on
  specifics.
- Several docs reference GitHub issue numbers (e.g., `#5489`, `#5524`) for
  historical context on convention decisions — these issues may be closed.
- `admin/todo-old-download-links` is a leftover text file (not a directory)
  listing download sites that need updating, with a reference to GitHub issue #6461.
