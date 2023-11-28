#!/bin/bash

./gradlew shadowJar
docker compose -f .docker/docker-compose.yml --ansi never -p triplea up
