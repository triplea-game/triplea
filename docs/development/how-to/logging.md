# Logging

## Where the logs go

| Where | What you get |
|---|---|
| The terminal running the game | Everything, from `DEBUG` up |
| `triplea/triplea.log` in your home directory | The same in a file, rolled over daily, kept 3 days |
| A dialog in the game | Every `WARN` and `ERROR` |

The file is `~/triplea/triplea.log` on Linux and macOS and `C:\Users\<you>\triplea\triplea.log`
on Windows. An installed TripleA writes to the same file.

Follow the file live:

```bash
tail -f ~/triplea/triplea.log                                        # Linux, macOS, Git Bash
Get-Content $env:USERPROFILE\triplea\triplea.log -Wait -Tail 50      # PowerShell
```

## Logging from your code

Classes use Lombok's `@Slf4j`:

```java
@Slf4j
public class MyClass {
  void doSomething(final Territory territory) {
    log.debug("Moving into {}", territory.getName());
  }
}
```

Use `debug` and `info` for developer output. `warn` and `error` open a dialog for the player,
so keep them for messages a player should see; see
[Error Handling And Logging](../error-handling-and-logging.md).

## What the AI is thinking

Make a player "Hard (AI)". The game then gets a **Debug** menu, and
**Debug → Hard AI → Show Logs** shows the AI's reasoning live.
