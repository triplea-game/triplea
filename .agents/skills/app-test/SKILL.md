---
name: app-test
description: Manually test the desktop client by starting it and clicking through it like a player (screenshots, mouse, keyboard). ONLY when the user explicitly asks for it (e.g. "/app-test", "click through the game", "test it in the app"); never on your own initiative, it takes over the user's mouse and screen.
---

# App test (click through the running game)

This drives the user's real desktop. Only run it when the user asked for it, and tell them
before you start that the game window will take focus and they should leave mouse and keyboard
alone until you report back.

## 1. Plan

Turn the request into a short scenario with expected results, e.g. "start a local game on the
tutorial map with all sides Hard (AI), let one round run: no error dialog, units move". If the
user named nothing specific, use that default. For pure engine behaviour an all-AI game without
UI is often enough: `./gradlew :smoke-testing:test` (see `game-app/smoke-testing/AGENTS.md`).

## 2. Start the game

Needs JDK 25 (see the `build` skill). Start it **in the background** so you keep control, with
its output in a log:

```bash
mkdir -p build/app-test
./gradlew :game-headed:run --console=plain > build/app-test/run.log 2>&1   # run in background
python .agents/scripts/desktop.py wait TripleA --timeout 300              # first build takes minutes
```

## 3. Drive it

`.agents/scripts/desktop.py` (Windows, needs Pillow) is the hands and eyes:

| Step | Command |
|---|---|
| See the window | `python .agents/scripts/desktop.py shot TripleA`, then open the printed PNG |
| Click a spot from that picture | `python .agents/scripts/desktop.py click 412 230` (`--double`, `--right`) |
| Type / press keys | `... type "Player 1"`, `... key enter`, `... key alt+f4` |
| Scroll the map | `... scroll 600 400 -3` |
| Find dialogs | `... windows` (dialogs are separate windows: `shot "<title>"`) |

Work in small loops: screenshot → decide one action → act → screenshot to confirm. Coordinates
always come from the **latest** screenshot (it is scaled; the script maps back to the screen).
Never click outside the game's windows.

Useful places: *Start Local Game* → *Select Game* → set sides to "Hard (AI)" → *Play*. A fresh
setup has no maps: accept the tutorial map or use *Download Maps* (say so in the report). With a
Hard AI player the game has a *Debug* menu (*Debug → Hard AI → Show Logs*).

## 4. Watch the logs

- `build/app-test/run.log`: everything the game logs while you click.
- `~/triplea/triplea.log`: the same, kept across runs.
- Any `WARN`/`ERROR` also opens a dialog in the game: screenshot it, note the text, close it.
- Known noise: `com.apple.eawt.event`, `Latest Version Response ... 404`, version `0.0+00000`.

## 5. Finish

Close the game (*File → Exit* or `key alt+f4` while it has focus, confirm the dialog) and check
with `windows` that it is gone. If it hangs, stop the background Gradle run you started; do not
kill other `java` processes (the IDE runs on Java too). Delete `build/app-test/shot-*.png` that
show nothing of interest.

Report: the scenario, each step with what you saw versus what you expected, errors from the log
(quote them), and the screenshot paths of anything wrong.

On Linux or macOS `desktop.py` does not work: use the tool's own screen/computer-use feature if
it has one, otherwise `xdotool` and `import` (ImageMagick) the same way, or tell the user.
