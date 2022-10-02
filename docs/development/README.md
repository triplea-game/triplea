# Developer Setup Guide

## Before Getting Started
- Install JDK 11 (project is on this Java version)
- [Install IDE](./how-to/ide-setup) (favor choosing IDEA)
  - Create as a gradle project (file > open project > select the build.gradle file))
  - Usually TripleA and lobby are started from within IDE, look for checked in 'run configurations'.
- Install docker
  - Docker for Mac can be obtained at: <https://store.docker.com/editions/community/docker-ce-desktop-mac>

## Getting Started

- Fork: <https://github.com/triplea-game/triplea>
- Using your favorite git-client, clone the newly forked repository 
- Setup IDE: [/docs/development/how-to/ide-setup](how-to/ide-setup)
- Use a feature-branch workflow, see: [typical git workflow](reference/typical-git-workflow.md))
- Submit pull request, see: [pull requests process](../project/pull-requests.md).

## Compile and launch TripleA

```bash
./gradlew :game-app:game-headed:run
```

For more detailed steps on building the project, see:
- [how-to/cli-build-commands.md](reference/cli-build-commands.md)

TripleA can also be launched from IDE as well, there  are run-configurations
checked into the code that the IDE should automatically find.

## Launch Local Database

```bash
# requires docker to be installed
./spitfire-server/database/start_docker_db
```

This will launch a postgres database on docker and will install the latest
TripleA schema with a small sample dataset for local testing.

After the database is launched you can:
- run the full set of tests
- launch a local lobby

## Running all tests & checks locally before PR

The verify script will execute all checks done as part of the PR
builds. The verify script will launch a local database if it is not
already running. 
```
cd .../triplea/
./verify
```

## Run Formatting

We use 'google java format', a plugin can be installed to IDE to properly format
from IDE. Everything can be formatted as well from CLI:

```
./gradew spotlessApply
```

## Launch local lobby:

Lobby can be launched via the checked-in run configurations from IDE, or from CLI:
```bash
./gradlew :spitfire-server:dropwizard-server:run
```

To connect to local lobby, from the game client:
  - 'settings > testing > local lobby'
  - play online
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

## Code Conventions

Full list of coding conventions can be found at: [reference/code-conventions](./reference/code-conventions)

## Deployment & Infrastructure Development

The deployment code is inside of the '[/infrastructure](./infrastructure)' folder.

We use [ansible](https://www.ansible.com/) to execute deployments.

You can test out deployment code by first launching a virtual machine and then running a deployment
against that virtual machine. See '[infrastructure/vagrant/REAMDE.md](./infrastructure/vagrant/REAMDE.md)'
for more information.

# Pitfalls and Pain Points to be aware of

## Save-Game Compatibility

- Do not rename private fields or delete private fields of anything that extends `GameDataComponent`

Game saves are done via object serialization that is then written to file. Renaming or deleting
fields will prevent previous save games from loading.

## Network Compatibility

'@RemoteMethod' indicates methods invoked over network. The API of these methods may not change.

## Lots of Manual Testing Required

A lot of code is not automatically verified via tests, any change should be tested pretty
thoroughly and for a variety of maps and scenarios. This can be very time consuming for
even the smallest of changes.

