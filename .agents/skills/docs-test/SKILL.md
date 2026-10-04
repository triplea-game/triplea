---
name: docs-test
description: Test the documentation - broken links, wrong Gradle project paths, and (on request) dry-run every documented gradlew command. Use when asked to test, validate or lint the docs, after editing docs, or when a documented command fails.
---

# Docs test

`docs-check` asks "does the doc still say what the code does?". This skill asks "do the links
and commands in the docs actually work?". Both are needed; the second one is mechanical.

1. Fast static test (seconds, no Gradle):

   ```bash
   python .agents/scripts/docs_test.py                      # every tracked .md file
   python .agents/scripts/docs_test.py AGENTS.md docs/development/README.md   # only these
   ```

   It reports relative links to missing files and `gradlew` task paths whose project does not
   exist (projects are flat: `:game-core`, never `:game-app:game-core`).

2. Command test (minutes, needs JDK 25, see the `build` skill), when commands were changed or the
   user asks for a full test:

   ```bash
   python .agents/scripts/docs_test.py --gradle AGENTS.md docs/development/README.md
   ```

   Every distinct `gradlew ...` line inside a code block runs with `--dry-run`: unknown projects,
   tasks and options fail without building anything. Lines with `<placeholders>` are skipped.

3. Fix what it finds with the smallest edit: correct the path or link; for a link to something
   that no longer exists, link the replacement or drop the link. Do not touch unrelated
   findings in files outside the task unless the user asked for a full docs test; list them
   instead.

4. Report: the command(s) you ran, what you fixed, and the findings you left with their file
   and line.
