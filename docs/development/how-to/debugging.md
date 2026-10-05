# Debugging

Start the game, or a test, with a debug port open, then attach any Java debugger to it. This
works the same with every IDE and on every OS.

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

If `localhost` refuses to connect, use `127.0.0.1`. If the port is taken, an earlier game is
probably still running; end that `java` process.

While the game is stopped at a breakpoint, its window freezes. That's expected.

IntelliJ users can also start the shared **HeadedGameRunner** run configuration in debug mode.
Launch through Gradle, not the IDE's own compiler: only the Gradle build puts the game images
on the classpath (see
[Game crashes after splash screen displayed](../README.md#game-crashes-after-splash-screen-displayed)).

## Debugging a test

fixOften that is faster than clicking through a whole game to reproduce something.

```bash
./gradlew :game-core:test --tests games.strategy.triplea.UnitUtilsTest --rerun --debug-jvm
```

Keep `--rerun`: without it, Gradle skips a test task that already passed with unchanged code,
and no debug port opens.

## Where to put your first breakpoints

| You want to see... | Look in |
|---|---|
| The game starting | `HeadedGameRunner.main` |
| A move | `MoveDelegate.performMove`, `MoveValidator` |
| A battle | `BattleDelegate`, `MustFightBattle` |
| Buying units | `PurchaseDelegate` |
| The AI planning | `AbstractProAi` (`purchase`, `move`) |

To see what the game logs while you debug, see [Logging](logging.md).
