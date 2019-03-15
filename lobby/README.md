# Lobby

This is the well-known running TripleA lobby server.


## Build

```
../gradlew release
```


## Run

### With java -jar
```
../gradlew run
```

### With docker
```
../gradlew jibDockerBuild
docker run -d --name=dev-lobby -p 3304:3304 lobby:1.10.dev
```

TODO: update below, from http-server component merge into lobby.

## Overview

This project is a server side component, a stand-alone http(s) server.

### Functionality Provided / Strategic fit in the TripleA project

We envision two kinds of client-server communication, over http or over websocket.
Currently, lobby-server, uses Java RMI, which we would like to deprecate as it 
can be difficult to work with for development.

This server will accumulate new features that make sense for http communication,
and will accumulate some migrated features from `lobby-server`. Some features may
land here while we are working on a way to get a framework for communication
over web-socket (TBD if it will be this project or a different one 
that will provide web-socket communication).


### Usage and Client Information

See the sub-project `http-client` (WIP)

Any common structured data objects transmitted between client-server 
will be in a project called `http-data` (WIP).

### Building

Done with the Gradle `release` command:
```
cd http-lobby
../gradlew release
ls build/artifacts/
```

### Running the server

Unzip the build artifact after buidling, then run with:
```
java -jar build/artifacts/http-lobby-1.9.0.0.dev-all.jar
```

### Http Server Framework

Two candidates at the moment for http server, intent is to build them in parallel
so we can see how they work out in our context and in-practice:

* Java Spark
* DropWizard

