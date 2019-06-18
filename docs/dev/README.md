## Prerequisites

- Local installation/environment setup, see [Dev Setup](setup/dev_setup.md) to get started.


## Building and Running the Code  / Typical gradle commands

Launch the headed-game client:
```
./gradlew run 
```

Perform all code formatting, checks and tests (run **this** to verify PR builds):
```
./verify
```

## Building installers

- Install [Install4j7](https://www.ej-technologies.com/download/install4j/files)
- Create a `triplea/gradle.properties` file with:
```
install4jHomeDir = /path/to/install4j7/
```
- Obtain install47 license key (can get from maintainers or an open-source one from install4j)
- Run release task
```
export INSTALL4J_LICENSE_KEY={License key here}
./gradlew release
```

Installers will be created in `triplea/build/releases/`


## Docker Images

The following project-specific Docker images, which may be useful during development and testing, are available:

  - [Lobby database](https://github.com/triplea-game/triplea/tree/master/lobby-db/Dockerfile)



## DB Data for Local Testing

Run:
```bash
./lobby-db/load_sample_data
```

