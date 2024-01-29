FROM openjdk:11-jre-slim-buster

EXPOSE 8080
ADD configuration.yml /
ADD build/libs/maps-server.jar /
CMD java -jar maps-server.jar server /configuration.yml
