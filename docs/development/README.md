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
./game-app/run/run
```

For more detailed steps on building the project with CLI, see:
- [reference/cli-build-commands.md](cli-build-commands.md)

## Compile and launch TripleA (IDEA)

IDE setup has the needed configurations for the project to compile. There are checked in launchers that
IDEA should automatically find.  Look in 'run configurations' to launch the game-client.

## Running all tests locally before PR (CLI)

```
./game-app/run/check
```

## Run Formatting

We use 'google java format', be sure to install the plugin in your IDE to properly format
from IDE. Everything can also be formatted from CLI:

```
./gradew spotlessApply
```

PR builds will fail if the code is not formatted with spotless.


## Code Conventions (Style Guide)

Full list of coding conventions can be found at: [reference/code-conventions](code-conventions)

Please be sure to check these out so that you can fit the general style of the project.

## Lobby Development
 
### Launch Local Database

Local database is needed to run the servers (lobby).

```bash
# requires docker to be installed
./spitfire-server/database/start_docker_db
```

This will launch a postgres database on docker and will install the latest
TripleA schema with a small sample dataset for local testing.

After the database is launched you can:
- run the full set of tests
- launch a local lobby

### Launch local lobby

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

## Lots of Manual Testing Required

A lot of code is not automatically verified via tests. If reasonable tests can be added, do so!
Generally though even the smallest of changes will need to be manually and thoroughly tested
in a variety of maps and scenarios.

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

IDEA needs a tweak to overcome a JDk9 problem. See the IDE setup for the needed config that needs
to be added to your vmoptions file.


