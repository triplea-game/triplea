#!/bin/bash

set -eux
../../gradlew clean shadowJar
docker build . -t bot
docker run \
   --network host \
   --env LOBBY_URI=http://localhost:3000 \
   -v /home/$USER/triplea/downloadedMaps:/downloadedMaps \
   bot
