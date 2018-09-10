package org.triplea.lobby.server.config;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.config.MemoryPropertyReader;

/**
 * Collection of {@link LobbyPropertyReader} instances suitable for various testing scenarios.
 */
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
