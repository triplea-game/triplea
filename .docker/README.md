# Docker

This folder provides Dockerfiles for building various TripleA component images.

## lobby-db-dev.df

Dockerfile for building a lobby database image that can be used for development/testing.

### Build

Build the image using the following command (run from the root of your Git repository):

```
$ docker build --tag triplea/lobby-db:latest -f .docker/lobby-db-dev.df .
```

### Run

Start a new container using the following command:

```
$ docker run -d --name=triplea-lobby-db -p 5432:5432 triplea/lobby-db
```
