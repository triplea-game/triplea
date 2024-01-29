FROM openjdk:11-jre-slim-buster

EXPOSE 8080
ADD configuration.yml /
ADD build/libs/triplea-dropwizard-server.jar /
CMD java -jar triplea-dropwizard-server.jar server /configuration.yml
