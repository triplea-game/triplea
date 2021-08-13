# Local Build And Testing

All commands assume a working directory of the triplea clone. EG: `cd ~/work/; git clone git@github.com....triplea; cd ./triplea`

To build the TripleA client on the command line execute
```
./gradlew :game-app:game-headed:build
```
To run the client issue
```
./gradlew :game-app:game-headed:run
```

Docker for Mac can be obtained at: https://store.docker.com/editions/community/docker-ce-desktop-mac

## Start Dev Database

[launch-local-database.md](launch-local-database.md])

## Running all checks & tests

```
./verify
```

To run just static analysis checks (checkstyles & PMD & custom checks), run:
```
./verify-no-tests
```

## Running a specific test case

To run all tests of the (sub)project `game-core` use
```
./gradlew :game-app:game-core:test
```

To run only the tests of a given class (`UnitUtilsTest`) use
```
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest
```

To run a given test method, here `multipleTransportedUnitsAreTransferred()` use:
```
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest.multipleTransportedUnitsAreTransferred
```

Some wildcards are also supported by gradle:
```
./gradlew :game-app:game-core:test --tests 'games.strategy.triplea.UnitUtilsTest.*Units*'
```
(Be aware of a potential shell expansion of `*` and quote it properly.)

*Note:* `gradle` uses caches heavily, thus, if nothing has changed, reruning a test will not actually run the test again but only recall its cached result.  To really re-execute a test you must run the task `:clean` or `:cleanTest` beforehand and supply the `--no-build-cache` option:
```
./gradlew --no-build-cache :game-app:game-core:cleanTest :game-app:game-core:test
```

## Formatting

```
./gradlew spotlessApply
```

To format everything, including documentation files run:
```
./format
```

## Testing a local lobby

[launch-local-lobby.md](launch-local-lobby.md)

* Start server by using the checked-in IDE launcher 'dropwizard-server'
* Start a bot, look for the headless game launcher, the bot will connect to your local lobby
* Start the client, look for headed-game client launcher
* In engine settings, go to test, select "use local lobby"
* Click play online, log in without an account or use the predefined
  moderator account "test:test"
