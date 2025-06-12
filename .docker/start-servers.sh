#!/bin/bash

# Use docker to start local TripleA servers

scriptDir="$(dirname "$0")"
set -eu
(
  cd "$scriptDir" || exit 1
  docker compose pull
  LOBBY_PORT=5000 docker compose up --detach
  echo "lobby started on port 5000"
)
