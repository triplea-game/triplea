#!/bin/bash

set -e

./gradlew shadowJar
docker compose -f .docker/docker-compose.yml --ansi never -p triplea up
