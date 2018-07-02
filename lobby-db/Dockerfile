#
# Dockerfile for building a lobby database image that can be used for development/testing.
#
# This image MUST NOT be used for production!
#

FROM postgres:9.5
MAINTAINER "tripleabuilderbot@gmail.com"

ENV POSTGRES_DB=ta_users

COPY ["./src/main/resources/db/migration", "/docker-entrypoint-initdb.d"]
