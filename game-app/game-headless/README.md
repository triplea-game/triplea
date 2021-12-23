# Headless Game Server

A headless game server for TripleA, also known as a _bot_.

## Run

Example command to run a new headless game server from Gradle:

```
$ ./gradlew :game-headless:run --args=' \
    -Ptriplea.game= \
    -Ptriplea.lobby.game.comments=automated_host \
    -Ptriplea.lobby.game.hostedBy=Bot1_TestServer \
    -Ptriplea.lobby.game.supportEmail=developer@gmail.com \
    -Ptriplea.lobby.host=127.0.0.1 \
    -Ptriplea.lobby.port=3304 \
    -Ptriplea.map.folder=/home/me/triplea/downloadedMaps \
    -Ptriplea.name=Bot1_TestServer \
    -Ptriplea.port=3300 \
    -Ptriplea.server=true \
    '
```
