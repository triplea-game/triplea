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
