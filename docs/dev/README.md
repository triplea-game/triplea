## Prerequisites

- Local installation/environment setup, see [Dev Setup](setup/dev_setup.md) to get started.


## Building and Running the Code  / Typical gradle commands

Apply project formatting and cleanups:
```
./gradlew spotlessApply
```

Run tests:
```
./gradlew test
```

Launch the headed-game client:
```
./gradlew run 
```

Perform all code checks and tests (run **this** before submitting PR):
```
./gradlew --parallel check
```

## Docker Images

The following project-specific Docker images, which may be useful during development and testing, are available:

  - [Lobby database](https://github.com/triplea-game/triplea/tree/master/lobby-db/Dockerfile)

