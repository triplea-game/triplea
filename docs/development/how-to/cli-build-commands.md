This page serves as reference for the various
CLI build commands that can be run.

## Running Tests via CLI

```
# Run tests for a (sub)project
./gradlew :game-app:game-core:test

# Run a specific test
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest

# Runs a specific  test method
./gradlew :game-app:game-core:test --tests games.strategy.triplea.UnitUtilsTest.multipleTransportedUnitsAreTransferred

# Run specific tests using wildcard (be sure to use quotes around wildcard)
./gradlew :game-app:game-core:test --tests 'games.strategy.triplea.UnitUtilsTest.*Units*'
```

`gradle` uses caches heavily, thus, if nothing has changed, reruning a test will not actually run the test again but only recall its cached result.  To really re-execute a test you must run the task `:clean` or `:cleanTest` beforehand and supply the `--no-build-cache` option:
```
./gradlew --no-build-cache :game-app:game-core:cleanTest :game-app:game-core:test
```

