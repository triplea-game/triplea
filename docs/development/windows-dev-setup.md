# Windows Dev Setup

**Goal:** get TripleA running from *your* code on Windows, see what it logs, and step through
it with a debugger.

**Time:** 30 to 60 minutes. Most of it is waiting for downloads.

**What you don't need:** WSL, Docker, a Gradle install, or any particular IDE. Everything here
runs from a terminal, and any Java IDE can take over from there.

---

## 1. Three things to install

| What | Get it | Watch out |
|---|---|---|
| **Git for Windows** | [git-scm.com](https://git-scm.com/download/win) | Keep the default *"Git from the command line and also from 3rd-party software"*. The build calls `git` itself. |
| **JDK 25** | [Temurin 25 (.msi)](https://adoptium.net/temurin/releases/?version=25&os=windows) | It has to be **25**, not 21 and not 27. Tick **Set JAVA_HOME variable** in the installer. |
| **An editor or IDE** | Whatever you like: IntelliJ, VS Code, Eclipse, ... | Optional for building and running. |

Git for Windows comes with **Git Bash**. Use it for the repo's scripts (`./gradlew`,
`./verify`). PowerShell works for the Gradle commands too (`.\gradlew ...`).

### Is Java set up right?

`./gradlew` starts Gradle with the Java in `JAVA_HOME`, and the build compiles and runs the game
with JDK 25. With `JAVA_HOME` pointing at JDK 25, both are covered. Open a **fresh** terminal:

```powershell
# PowerShell
& "$env:JAVA_HOME\bin\java" -version
```

```bash
# Git Bash
"$JAVA_HOME/bin/java" -version
```

You want to see `25.x`. If you get an error or another version, set `JAVA_HOME` under *Start →
"Edit environment variables for your account"* to your JDK 25 folder, then reopen every
terminal and IDE. They only read environment variables when they start.

---

## 2. Grab the code

Fork [triplea-game/triplea](https://github.com/triplea-game/triplea) on GitHub, then clone
your fork to a short path **without spaces**:

```bash
cd /c/dev                                    # or any other short path without spaces
git clone https://github.com/<your-user>/triplea.git
cd triplea

git remote add upstream https://github.com/triplea-game/triplea.git
git config blame.ignoreRevsFile .git-blame-ignore-revs
git config --global core.longpaths true     # Windows + long paths = pain otherwise
```

**The 10-second tour:**
- `game-app/game-headed` is the game window you'll run (main class
  `org.triplea.game.client.HeadedGameRunner`).
- `game-app/game-core` is where most of the engine lives.
- Gradle names are flat: `:game-headed`, `:game-core`. `:game-app:game-core` does **not** exist.

---

## 3. Fire it up

```bash
./gradlew :game-headed:run
```

The first time, go grab a coffee. Gradle downloads itself, all the libraries and about 100 MB
of game images and sounds. After that, startup takes seconds.

When the TripleA window pops up, you're running your own code. 🎉

**No maps yet?** A fresh setup has none. Say **yes** when the game offers the tutorial map, or
click **Start Local Game → Download Maps**. Then **Select Game**, make one side "Hard (AI)", and
hit **Play**.

**Can be ignored:**
- `package com.apple.eawt.event not in java.desktop`, a macOS-only flag.
- `Latest Version Response ... 404`, plus the version showing as `0.0+00000`. That's normal
  for local builds.

Your maps, save games and logs live in `C:\Users\<you>\triplea\`. An installed TripleA uses
the same folder.

---

## 4. Logs: where to look

| Where | What you get |
|---|---|
| The terminal running the game | Everything, from `DEBUG` up |
| `C:\Users\<you>\triplea\triplea.log` | Same thing in a file (kept 3 days) |
| A pop-up in the game | Every `WARN` and `ERROR` |

Follow the file live:

```bash
tail -f ~/triplea/triplea.log                                        # Git Bash
Get-Content $env:USERPROFILE\triplea\triplea.log -Wait -Tail 50      # PowerShell
```

**Log something yourself.** Classes use Lombok's `@Slf4j`:

```java
@Slf4j
public class MyClass {
  void doSomething(final Territory territory) {
    log.debug("Moving into {}", territory.getName());
  }
}
```

Stick to `debug` and `info` for your own output. `warn` and `error` throw a dialog in the
player's face, so save them for real problems.

**Too noisy, or not enough detail?** Add a line to
`game-app/game-headed/src/main/resources/logback.xml`. Keep it local and don't commit it:

```xml
<logger name="games.strategy.triplea.delegate" level="trace"/>
```

**Curious what the AI is thinking?** Make a player "Hard (AI)". The game then gets a **Debug**
menu, and **Debug → Hard AI → Show Logs** shows its reasoning live.

---

## 5. Debugging

The trick that works with every IDE: start the game with a debug port open, then attach to it.

```bash
./gradlew :game-headed:run --debug-jvm
```

The game now **waits** on port `5005` and opens only after a debugger connects. Attach with
whatever you use:

| Tool | How to attach |
|---|---|
| IntelliJ | *Run → Edit Configurations → + → Remote JVM Debug*, port `5005` |
| VS Code (Java extension pack) | Add to `launch.json`: `{ "type": "java", "request": "attach", "name": "TripleA", "hostName": "127.0.0.1", "port": 5005 }` |
| Eclipse | *Debug Configurations → Remote Java Application*, port `5005` |
| No IDE at all | `jdb -connect com.sun.jdi.SocketAttach:hostname=127.0.0.1,port=5005`, then `cont` |

If `localhost` refuses to connect, use `127.0.0.1`.

**Where to put your first breakpoints:**

| You want to see... | Look in |
|---|---|
| The game starting | `HeadedGameRunner.main` |
| A move | `MoveDelegate.performMove`, `MoveValidator` |
| A battle | `BattleDelegate`, `MustFightBattle` |
| Buying units | `PurchaseDelegate` |
| The AI planning | `AbstractProAi` (`purchase`, `move`) |

While you're stopped at a breakpoint, the game window freezes. That's expected.

**Debugging a test** works the same way:

```bash
./gradlew :game-core:test --tests games.strategy.triplea.UnitUtilsTest --debug-jvm
```

Often that's faster than clicking through a whole game to reproduce something.

---

## 6. Using an IDE

Open the repo folder as a **Gradle project** and you're mostly done. Three things to check:

1. **The IDE knows about JDK 25.** Set the project JDK to 25 so the editor understands the
   code, and let the IDE run Gradle with it as well (IntelliJ: *Settings → Build Tools →
   Gradle → Gradle JVM*).
2. **Gradle does the building.** The game images only get onto the classpath through the
   Gradle build. If your IDE compiles on its own, the game crashes right after the splash
   screen. When in doubt, run with `./gradlew :game-headed:run --debug-jvm` and attach.
3. **Formatting is Google Java Format.** Most IDEs have a plugin for it. Or just run
   `./gradlew spotlessApply` before you commit.

IntelliJ users get a ready-made **HeadedGameRunner** run configuration from the repo, plus some
extra tips in [ide-setup/intellij-setup.md](ide-setup/intellij-setup.md).

---

## 7. Smoke test: does it all work?

1. In `HeadedGameRunner.main`, add `log.info("Hello from my build");`.
2. Run the game and find your line in the terminal and in `triplea.log`.
3. Start it with `--debug-jvm`, attach, put a breakpoint on that line, and watch it stop.
4. Clean up: `git checkout -- game-app/game-headed`

All four worked? You're set up. 🚀

---

## 8. Before you open a PR

```bash
./gradlew :game-core:test                                       # tests for one module
./gradlew :game-core:test --tests games.strategy.triplea.UnitUtilsTest
./gradlew spotlessApply                                         # fix formatting
./verify                                                        # everything, like CI (slow)
```

**Two rules that will bite you:**
- Don't rename, delete or move private fields in classes that extend `GameDataComponent`.
  Save games are Java-serialized, and you'd break everyone's saves.
- Don't touch methods marked `@RemoteActionCode`. They're called over the network.

Branches and PRs: [typical-git-workflow.md](typical-git-workflow.md),
[pull-requests.md](../project/pull-requests.md).

---

## 9. Something's broken

| You see | Do this |
|---|---|
| `JAVA_HOME is not set` or `Cannot find a Java installation ... languageVersion=25` | Gradle can't find JDK 25. Point `JAVA_HOME` at it (section 1) and open a new terminal. In an IDE, set its Gradle JVM to JDK 25 (section 6). |
| Gradle can't run `git` | Git isn't on the PATH. Reinstall Git with the "3rd-party software" option. |
| Game crashes right after the splash screen | Images are missing. Let Gradle do the build (section 6), or run `./gradlew :game-headed:processResources`. |
| `$'\r': command not found` | You ran the scripts from WSL on a Windows checkout. Use Git Bash. |
| `Filename too long` | `git config --global core.longpaths true` |
| Debugger won't connect | Use `127.0.0.1`, or an old game is still holding port 5005. Kill `java.exe` in Task Manager. |
| Everything is sooo slow | Windows Defender is scanning every file. Exclude the repo and `%USERPROFILE%\.gradle`. |

Still stuck? Ask in the [forum](https://forums.triplea-game.org/) or open a GitHub issue.
