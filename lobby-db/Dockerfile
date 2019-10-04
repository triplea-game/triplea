#
# Dockerfile for building a lobby database image that can be used for development/testing.
#

FROM postgres:9.5
MAINTAINER "tripleabuilderbot@gmail.com"

ENV POSTGRES_USER=lobby_user
ENV POSTGRES_DB=lobby_db

# TODO: Copy a production-like snapshot. Snapshots include the flyway tracking tables, 
# running flyway would then be incremental and run only new migrations, better simulating
# production.
#
# COPY ["./src/main/resources/snapshots/SNAPSHOT-FILE", "/docker-entrypoint-initdb.d"]
