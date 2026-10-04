# .agents - shared setup for AI coding agents

Tool-neutral workflow for any coding agent (Claude Code, Codex, Gemini CLI, Copilot, Cursor,
...). The root `AGENTS.md` and the nested `AGENTS.md` files describe the code; this folder says
how to work on it. Tool-specific wiring (for example Claude Code hooks in `.claude/`) only calls
into these files.

Layers: `AGENTS.md` describes (what is where, why), `rules/` prescribes (MUST/NEVER, checked by
lint, build or review), `docs/` is deep reference read on demand: look a doc up in
`docs-index.md` (one line per doc) before opening it. When an `AGENTS.md` or doc disagrees with
the code, the code wins and the doc gets fixed.

| Path | What |
|---|---|
| `rules/` | Requirements per area. Read the ones matching the files you change: `java.md` (`**/*.java`), `tests.md` (`src/test`, `src/testFixtures`), `compatibility.md` (`game-app/`, `http-clients/`: save games, network), `shell.md` (`*.sh`, `.build/`, `verify`), `docs-map.md` (which doc to read for which code), `git.md` (always). |
| `skills/` | Task recipes in the Agent Skills format (`<name>/SKILL.md`): `build`, `docs-check`, `docs-test`, `agents-audit`, `app-test` (user-requested only). |
| `agents/` | Role prompts: `triplea-developer` (implements a scoped task), `triplea-reviewer` (read-only review, answers `VERDICT: pass \| changes needed`), `agents-md-auditor` (fact-checks docs). Run them as subagents with a fresh context if your tool has subagents, otherwise follow them yourself. |
| `scripts/` | `check_changes.py` (all checks for the current changes), `convention_lint.py`, `docs_test.py`, `desktop.py` (screenshots and clicks for `app-test`). Python 3, no dependencies except Pillow for `desktop.py`. |
| `docs-index.md` | One line per file in `docs/`. |
| `local.md` | Optional machine notes (JDK path, ...). Not committed; never put shared knowledge here. |

## Workflow for a code change

1. Read the `AGENTS.md` files from the root down to the files you change, and the matching
   `rules/`.
2. Implement the smallest complete change, with a test for any behaviour change
   (`agents/triplea-developer.md`).
3. Simplify: remove anything the task did not need (unused options, abstractions, files).
4. Check: `python .agents/scripts/check_changes.py` must end green. It runs the convention lint
   and the docs test on the added lines, then for every changed module `spotlessApply`,
   `test`, `check` (Checkstyle, PMD) and check-custom-style.
5. Docs: `skills/docs-check` - fix every `AGENTS.md` / `docs/` statement the change made wrong.
6. Review with fresh eyes: `agents/triplea-reviewer.md` until `VERDICT: pass`, then a
   correctness review of the diff (bugs, edge cases).
7. Do not commit, stage or push (`rules/git.md`); end with a short summary for the commit
   message.

## How tools pick this up

- Every tool that reads `AGENTS.md` is pointed here from the root `AGENTS.md`.
- Codex discovers `.agents/skills/` on its own.
- Claude Code: `.claude/` (if present) holds thin wrappers for the skills, rules and
  subagents, plus hooks that enforce the workflow above at the end of each turn.
