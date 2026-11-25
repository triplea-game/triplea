FROM eclipse-temurin:25-jre

ENV BOT_PORT_NUMBER=4000
EXPOSE $BOT_PORT_NUMBER

ENV BOT_NAME=BotDocker01
ENV LOBBY_URI=http://localhost:3000
ENV BOT_MAX_MEMORY="256M"
ENV BOT_XSS="1250k"

ENV MAPS_FOLDER=/downloadedMaps
ENV EXIT_ON_GAME_END=FALSE

RUN mkdir $MAPS_FOLDER
RUN useradd -u 999 bot
RUN chown bot:root $MAPS_FOLDER

RUN mkdir -p /home/bot/triplea/savedGames/autoSave
RUN chown -R bot:root /home/bot/

USER bot

ADD build/libs/game-headless.jar /
COPY --chmod=755 entrypoint.sh /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
