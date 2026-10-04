---
name: build
description: Build, test, format and run static checks with Gradle (JDK 25 setup, single module/test selection, verify). Use for any "build", "compile", "run the tests", "check", "format" request in this repo.
---

# Build and test

## JDK 25

The build uses a Gradle toolchain for **JDK 25** and cannot download one, so a JDK 25 must be
installed. `./gradlew` starts with the Java in `JAVA_HOME`; point it at JDK 25 and both are
covered. Check first:

```bash
"$JAVA_HOME/bin/java" -version          # Git Bash / Linux / macOS
```

```powershell
& "$env:JAVA_HOME\bin\java" -version    # PowerShell
```

If it is not 25: look in `.agents/local.md` (machine notes, not committed) or the usual install
folders (`C:\Program Files\Eclipse Adoptium`, `~/.jdks`, `/usr/lib/jvm`), and set `JAVA_HOME`
for the command. Never fall back to another JDK version; if there is no JDK 25, tell the user.

## Gradle project names

Projects are **flat**, named after their directory: `game-app/game-core` is `:game-core`,
`lib/java-extras` is `:java-extras` (see `settings.gradle.kts`). `:game-app:game-core` does not
exist. check-custom-style takes the directory instead.

## Commands

```bash
./gradlew :game-core:test                                       # tests of one module
./gradlew :game-core:test --tests games.strategy.triplea.UnitUtilsTest
./gradlew :game-core:spotlessApply :game-core:check             # format + tests + Checkstyle + PMD
.build/code-convention-checks/check-custom-style game-app/game-core   # grep checks for one module
./verify                                                        # everything, as CI does (slow)
./gradlew :game-headed:run                                      # start the desktop client
python .agents/scripts/check_changes.py                         # all checks for the current changes
```

In PowerShell use `.\gradlew` and `$env:JAVA_HOME = "..."`.

Notes:

- Always scope to the changed modules; `./gradlew test` runs everything and takes long.
- `check` already includes `test` and `spotlessCheck`; run `spotlessApply` first so formatting
  never fails it.
- `check_changes.py` runs lint, docs test, `spotlessApply`, `test`, `check` and
  check-custom-style for every module with changed sources; its Gradle log is
  `build/check-changes.log`.
- The first run starts the Gradle daemon and takes about a minute before any task.
