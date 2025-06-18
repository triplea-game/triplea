#!/bin/bash

# Use docker to start local TripleA servers

scriptDir="$(dirname "$0")"
set -eu

(
  cd "$scriptDir" || exit 1
  docker compose down
)
