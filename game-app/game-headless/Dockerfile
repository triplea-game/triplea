FROM openjdk:11-jre-slim-buster

ENV BOT_PORT_NUMBER=4000
EXPOSE $BOT_PORT_NUMBER

ENV BOT_NAME=BotDocker01
ENV LOBBY_URI=http://localhost:3000
ENV BOT_MAX_MEMORY="256M"
ENV BOT_XSS="1250k"

ENV MAPS_FOLDER=/downloadedMaps
ENV EXIT_ON_GAME_END=FALSE

RUN mkdir $MAPS_FOLDER

ADD build/libs/triplea-game-headless.jar /
CMD java \
   -Xmx$BOT_MAX_MEMORY \
   -Xss$BOT_XSS \
   -Dtriplea.name=$BOT_NAME \
   -Dtriplea.port=$BOT_PORT_NUMBER \
   -Dtriplea.lobby.uri=$LOBBY_URI \
   -Dtriplea.exit.on.game.end=$EXIT_ON_GAME_END \
   -jar triplea-game-headless.jar
