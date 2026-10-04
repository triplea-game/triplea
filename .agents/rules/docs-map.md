---
paths:
  - "game-app/**"
  - "http-clients/**"
---

# Where the docs for this code are

`docs/` is not loaded automatically. Before changing code in these areas, read the matching
doc. For anything not in this table, check `.agents/docs-index.md` (one line per doc) before
opening docs. Docs may lag behind the code: when a doc and the code disagree, the code wins and the doc
gets fixed (`docs-check`).

| Touching | Read first |
|---|---|
| Map XML parsing, `map-data`, `games/strategy/triplea/attachments/`, game XML resources | `docs/development/map-loading-design.md`, `docs/map-making/tutorial/creating-custom-map-xml.md`; new map features go into `docs/map-making/reference/map-features-change-log.md` |
| `map.yml` handling | `docs/map-making/reference/map-yml-file.md` |
| Delegates, `GameData`, `Change`/`ChangeFactory`, players | `docs/development/engine-code-overview.md` (old; verify against code), `docs/development/glossary.md` |
| Rule variants (v1, v2, ... rule books) | `docs/development/game-rule-sets.md` |
| `game-app/ai` | `docs/development/ai-overview-and-backlog.md` |
| Error dialogs, logging levels | `docs/development/error-handling-and-logging.md` |
| User-visible strings, resource bundles | `docs/development/i18n_languages.md` |
| Sounds, images, the assets folder | `docs/development/game-asset-management.md` |
| `http-clients/`, lobby login | `docs/development/lobby-authentication-logic.md` |
