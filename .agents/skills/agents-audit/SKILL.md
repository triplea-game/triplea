---
name: agents-audit
description: Fact-check all AGENTS.md files (optionally key docs/ files) against the code with the agents-md-auditor role, in parallel where the tool allows, then fix what they find. Use when asked to audit, verify, refresh or "solidify" the AGENTS.md files or docs.
---

# AGENTS.md audit

1. List the files: `git ls-files '*AGENTS.md'`. Add the key docs from `docs/AGENTS.md`
   ("Key Documents for AI Agents") only when the user asked for docs too.
2. Split them into about 4 batches of similar size, keeping a directory and its children in the
   same batch (e.g. root + `.build` + `gradle` + `docs`; `game-app/game-core/**`; the other
   `game-app/*`; `lib/**` + `http-clients/**`).
3. Give each batch to the `agents-md-auditor` role (`.agents/agents/agents-md-auditor.md`): one
   subagent per batch, all started at once, when your tool has subagents; otherwise work through
   the batches yourself with that prompt. Pass the exact file list. Run
   `python .agents/scripts/docs_test.py` once as well: it finds broken links and wrong Gradle paths.
4. Collect the reports. Spot-check every `[wrong]` finding yourself before acting on it; drop
   findings you cannot confirm.
5. Fix with the smallest change that makes the statement true:
   - `[wrong]` -> correct it.
   - `[brittle]` -> replace line numbers and counts with names (file, class, task).
   - `[missing]` -> add one or two lines only where an agent editing that directory would
     otherwise go wrong.
   Keep each file's structure and tone; do not move prescriptive rules into AGENTS.md (they live
   in `.agents/rules/`).
6. These files are tracked upstream: leave the changes uncommitted and end with a per-file
   summary (what was wrong, what changed) the user can use for a PR.
