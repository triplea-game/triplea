# Database Testing

## DB Rider for Unit Testing

We use a framework called [DB Rider](https://github.com/database-rider/database-rider).
It sits on top of [DBUnit](http://dbunit.sourceforge.net/).

## Testing Requirements

DB testing requires a running docker container and for flyway to have been run to
deploy a schema to that container.

## Anatomy of DbUnit Test in TripleA

Example:

```
@RequiredArgsConstructor
class LobbyGameDaoTest extends LobbyServerTest {
  private final LobbyGameDao lobbyGameDao;

  @Test
  @DataSet(value = "game_hosting_api_key.yml", useSequenceFiltering = false)
  @ExpectedDataSet("lobby_games/expected/lobby_game_post_insert.yml")
  void insertLobbyGame() {
    lobbyGameDao.insertLobbyGame(
        ApiKey.of("HOST"),
        LobbyGameListing.builder() //
            .gameId("game-id")
            .lobbyGame(TestData.LOBBY_GAME)
            .build());
  }
```

A lot of config is driven from: `DropwizardServerExtension` which is extended by `LobbyServerTest`.
The extension will automatically inject 'Dao' (JDBI) classes into test constructors or test methods.
In the above it is the extension running for you `jdbi.onDemand(LobbyGameDao.class)` and then
injects it into the test via constructor (note the: `@RequiredArgsConstructor`)

### Automatic Table Cleanup

The `DropwizardServerExtension` will also clean up all data from all tables after every test.
This is done by looking for a file called 'db-cleanup.sql' that should list every table and
the order in which data should be removed from each.

### Datasets

Datasets are typically organized by table. Define one tables worth of data in one dataset file.
This is to help re-use.

Expectation datasets should be in a folder called 'expected' to keep the initial data apart from
expected outputs.

## Conventions
- tests are typically per DAO, DAO is a single class, usually organized around a single table
- dataset files are YML, lower snake case

