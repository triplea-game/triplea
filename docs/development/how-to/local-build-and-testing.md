# Local Build And Testing

## Assumptions

- Some familiarity with 'git' is assumed
- If using windows, you are using something like cygwin to run git commands

## Goals

- clone the code
- set up IDE
- install and start docker database (needed to run the lobby)
- build and run game client and local lobby via IDE
- verify script via CLI to do branch build verification


## Clone

Fork the main project repository: <https://github.com/triplea-game/triplea>

```bash
cd ~
mkdir work
cd work
git clone git@github.com....triplea
cd triplea
```

## Setup IDE

Setup IDE: [/docs/development/how-to/ide-setup](../how-to/ide-setup>)

## Start Dev Database

The dev database is needed to launch lobby and is also
needed for the full set of tests to pass.

Follow: [launch-local-database.md](launch-local-database.md])


## Launching the game and a local lobby with IDE

* Start server by using the checked-in IDE launcher 'dropwizard-server'
* Start a bot, look for the headless game launcher, the bot will connect to your local lobby
* Start the client, look for headed-game client launcher
* Once the game client is launched, update the in-game settings, "setings > test > use local lobby"
* Click play online, log in without an account or use the predefined
  moderator account "test:test"


## Build via CLI

Verify script will run all tests and static analysis checks,
run this before submitting a PR:
```bash
./verify
```

To run the game client:
```
./gradlew :game-app:game-headed:run
```

To run just static analysis checks (checkstyles & PMD & custom checks), run:
```
./verify-no-tests
```

### Formatting

```
./gradlew spotlessApply
```

To format everything, including documentation files run:
```
./format
```


## Additional References

* For more build commands, like running tests via CLI, see:
[how-to/cli-build-commands.md](./cli-build-commands.md)

* Typical git workflow commands to check in code:
[how-to/typical-git-workflow.md](./typical-git-workflow.md)

