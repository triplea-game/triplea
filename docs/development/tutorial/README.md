# NEW DEVELOPER GETTING STARTED!

- Setup IDE: [/docs/development/how-to/ide-setup](../how-to/ide-setup>)

## Further, typical git commands and submitting PRs

- [/docs/development/how-to/typical-git-workflow](../how-to/typical-git-workflow.md)
- [/docs/development/reference/dev-process/pull-requests.md](../reference/dev-process/pull-requests.md)

## Running the code

- IDE (there are launchers checked in for convenience)
- CLI: `./gradlew :game-app:game-headed:run`

## Running a local database
Follow: [/docs/development/how-to/launch-local-database.md](../how-to/launch-local-database.md)

This allows for a local server to be launched (also from IDE Or CLI), and the game-headed
client can be configured in 'settings > testing' to use the local server.

A local DB is also required for the 'verify' script to complete successfully.

```
./spitfire-server/database/start_docker_db
```

## Run verify script

```
./verify
```

Verify will run almost all of the PR checks locally. Verify builds the code, runs all tests,
runs code scanners like PMD, checkstyle, and runs a set of custom code scanners that
regex the code to enforce project specific code conventions.

It is also often useful to run code formatting:
```
./gradew spotlessApply
```
