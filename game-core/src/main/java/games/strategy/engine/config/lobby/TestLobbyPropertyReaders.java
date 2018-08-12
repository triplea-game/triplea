package games.strategy.engine.config.lobby;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.config.MemoryPropertyReader;

/**
 * Collection of {@link LobbyPropertyReader} instances suitable for various testing scenarios.
 */
// TODO: This class is intended only for use by test code. It is temporarily located in a production package while lobby
// server code is being gradually moved to the lobby project.
public final class TestLobbyPropertyReaders {
  /**
   * A lobby configuration suitable for integration testing.
   */
  public static final LobbyPropertyReader INTEGRATION_TEST = new LobbyPropertyReader(new MemoryPropertyReader(
      ImmutableMap.of(
          LobbyPropertyReader.PropertyKeys.POSTGRES_USER, "postgres",
          LobbyPropertyReader.PropertyKeys.POSTGRES_PASSWORD, "postgres")));

  private TestLobbyPropertyReaders() {}
}
