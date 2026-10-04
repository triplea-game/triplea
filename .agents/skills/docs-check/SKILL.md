---
name: docs-check
description: Check that AGENTS.md files and docs/ still match the code after a change, and update them. Part of finishing every code change; also use when asked to verify or refresh the docs.
---

# Docs check

Goal: no `AGENTS.md` or `docs/` file states something the change made false, and new things a
reader needs are written down. Keep doc edits as small as the code change.

1. List the changed files: `git status --short` and `git diff --stat HEAD`.
2. For each changed file, collect the docs that can describe it:
   - every `AGENTS.md` from the repo root down to the file's directory;
   - files under `docs/` that mention a changed class, method, module, Gradle task, command or
     config key: `grep -rn "<Name>" docs/ --include=*.md` (and `AGENTS.md` files the same way);
   - for build, check or convention changes: `.build/AGENTS.md`, `docs/development/code-conventions/`,
     `docs/development/build-overview-and-development.md`.
3. Read each candidate and compare it with the new code. Flag statements that are now wrong
   (renamed or moved classes, changed behaviour, removed or new commands, module dependencies,
   file tables, line numbers such as "build.gradle.kts lines 76-91").
4. Fix them. Add a short entry only where the change introduces something a reader of
   that doc would otherwise miss (new module, new command, new rule). Do not rewrite unrelated
   sections or document implementation details. Never write line numbers or counts into docs;
   name the file, class or task instead.
   If a new doc area appears (or one moves), update the table in `.agents/rules/docs-map.md`.
   When a doc is added, removed, renamed or its content changes meaningfully, update its line
   in `.agents/docs-index.md`.
5. Test the docs you touched: `python .agents/scripts/docs_test.py <changed .md files>` (links,
   Gradle project paths). If you changed a documented command, add `--gradle` to dry-run it.
6. Report in one or two lines: which docs were updated, or "docs checked, no changes needed"
   with the docs you looked at.
