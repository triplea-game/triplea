This page serves as reference for the various
CLI build commands that can be run.

## Running Tests via CLI

```
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

