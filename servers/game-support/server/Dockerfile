FROM openjdk:11-jre-slim-buster

EXPOSE 8080
ADD configuration.yml /
ADD build/libs/game-support-server.jar /
CMD java -jar game-support-server.jar server /configuration.yml
