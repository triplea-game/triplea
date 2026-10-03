#!/bin/sh
# exec replaces this shell so java runs as PID 1 and receives docker's SIGTERM
# directly; the bot's shutdown hook writes a final autosave on that signal.
set -eu

exec java \
  -Xmx"$BOT_MAX_MEMORY" \
  -Xss"$BOT_XSS" \
  -Dtriplea.name="$BOT_NAME" \
  -Dtriplea.port="$BOT_PORT_NUMBER" \
  -Dtriplea.lobby.uri="$LOBBY_URI" \
  -Dtriplea.exit.on.game.end="$EXIT_ON_GAME_END" \
  -jar /game-headless.jar \
  "$@"
