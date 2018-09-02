# Building and Running the Code

Below will run tests, second command will do verifications (checkstyle), last will run the project from source.
```
./gradlew test
./gradlew clean check
./gradlew run
```

First time contributors can follow [Dev Setup](setup/dev_setup.md) to get started.


## Docker Images

The following project-specific Docker images, which may be useful during development and testing, are available:

  - [Lobby database](https://github.com/triplea-game/triplea/tree/master/lobby-db/Dockerfile)



## Gradle

Checkstyle can be run with:

```
$ ./gradlew clean check
```

## Run Checkstyle checks individually:

```
$ ./gradlew clean checkstyleMain checkstyleTest checkstyleIntegTest
```

Checkstyle reports can be found within the folder `build/reports/checkstyle`.

You are **strongly encouraged** to run the `check` task before submitting a PR.
