FROM openjdk:11-jre-slim-buster

EXPOSE 4000
ENV BOT_NAME=BotDocker01
ENV LOBBY_URI=http://localhost:3000
RUN mkdir /downloadedMaps
ENV MAPS_FOLDER=/downloadedMaps

ADD build/libs/triplea-game-headless.jar /
CMD java \
   -Dtriplea.name=$BOT_NAME \
   -Dtriplea.port=4000 \
   -Dtriplea.lobby.uri=$LOBBY_URI \
   -jar triplea-game-headless.jar
