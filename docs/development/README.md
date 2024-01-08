# Developer Setup Guide

## Before Getting Started
- Install JDK 11 (project is using this Java version)
- [Install IDE](./how-to/ide-setup) (IDEA is better supported, YMMV with Eclipse)
  - Create as a gradle project (file > open project > select the build.gradle file))

## Mac

- Install docker: <https://store.docker.com/editions/community/docker-ce-desktop-mac>

## Windows

Set up WSL, this will give you a command line that can be used to run docker, gradle and the code check scripts.


## Getting Started

- Fork & Clone: <https://github.com/triplea-game/triplea>
- Setup IDE: [/docs/development/how-to/ide-setup](how-to/ide-setup)
- Use a feature-branch workflow, see: [typical git workflow](typical-git-workflow.md))
- Submit pull request, see: [pull requests process](../project/pull-requests.md).

If you are new to Open Source & Github:
  - https://docs.github.com/en/get-started/quickstart/contributing-to-projects
  - [Create SSH Key](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account)
    (usually you will not need to add the SSH key to your keychain, just create it and add it to github)

## Compile and launch TripleA (CLI)

```bash
# Build & Launch TripleA Game-Client
./gradlew :game-app:game-headed:run

# Run all build checks
./verify

# Run formatting
./gradew spotlessApply

# Launch a Postgres DB, build & launch TripleA Servers
./gradlew composeUp

# Connect to locally running database
./docker/connect-to-db.sh

# Runs all tests that do not require database
./gradlew test

./gradlew testWithDatabase

# Runs all tests
./gradlew allTest

# Run game-app tests
./game-app/run/check

# Run tests for a (sub)project
./gradlew :game-app:game-core:test

# Run a specific test
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest

# Runs a specific  test method
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest.multipleTransportedUnitsAreTransferred

# Run specific tests using wildcard (be sure to use quotes around wildcard)
./gradlew :game-app:game-core:test --tests 'games.strategy.triplea.UnitUtilsTest.*Units*'
```

`gradle` uses caches heavily, thus, if nothing has changed, re-running a test will not actually run the test again.
To really re-execute a test use the `--rerun-tasks` option:
```
./gradlew --rerun-tasks :game-app:game-core:test
```

## Run Formatting

We use 'google java format', be sure to install the plugin in your IDE to properly format from IDE.


## Code Conventions (Style Guide)

Full list of coding conventions can be found at: [reference/code-conventions](code-conventions)

## Lobby / Server Development

Run:
```
./gradlew composeUp
./gradlew :game-app:game-headed:run
``` 
Nginx will be running on port 80 following the 'composeUp'.
All requests are sent to NGINX and then routed to the correct
docker container.

To connect to local lobby, from the game client:
  - 'settings > testing > local lobby'
  - click play online button
  - use 'test:test' to login to local lobby as a moderator

### Working with database

```
## connect to database to bring up a SQL CLI
./spitfire-server/database/connect_to_docker_db

## connect to lobby database
\c lobby_db

## list tables
\d

## exit SQL CLI
\q

## erase database and recreate with sampledrop database schema & data and recreate
./spitfire-server/database/reset_docker_db
```


## Deployment & Infrastructure Development

The deployment code is inside of the '[/infrastructure](./infrastructure)' folder.

We use [ansible](https://www.ansible.com/) to execute deployments.

You can test out deployment code by first launching a virtual machine and then running a deployment
against that virtual machine. See '[infrastructure/vagrant/REAMDE.md](./infrastructure/vagrant/REAMDE.md)'
for more information.

# Pitfalls and Pain Points to be aware of

## Save-Game Compatibility

- Do not rename private fields or delete private fields of anything that extends `GameDataComponent`
- Do not move class files (change package) of anything that extends `GameDataComponent`

The above are to protect save game compatibility.  Game saves are done via Java object serialization. The serialized
data is binary and written to file. Changing any object that was serialized to a game data file will prevent the
save games from loading.

## Network Compatibility

'@RemoteMethod' indicates methods invoked over network. The API of these methods may not change.


# FAQ - common problems


### Assets folder not found

This is going to be typically because the working directory is not set properly. The 'run' gradle task
for game-headed will download game assets into the 'root' directory. When the game starts it expects
to find the assets folder in that root directory. If launching from IDE, chances are good the working
directory is not set.

Ideally the IDE launcher is checked in and pre-configured. This could be broken and needs to be 're-checked'
back in properly. 

In short:
- check working directory is 'game-app/game-headed'
- chect that `./gradlew downloadAssets` has been run and there is an 'assets' folder in the working directory

### Google formatter does not work

IDEA needs a tweak to overcome a JDk9 problem. The google java format plugin should show a warning dialog about
this if it is a problem.


