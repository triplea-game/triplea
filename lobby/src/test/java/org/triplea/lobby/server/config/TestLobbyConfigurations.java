package org.triplea.lobby.server.config;

import org.triplea.common.config.MemoryPropertyReader;

import com.google.common.collect.ImmutableMap;

/**
 * Collection of {@link LobbyConfiguration} instances suitable for various testing scenarios.
 */
public final class TestLobbyConfigurations {
  /**
   * A lobby configuration suitable for integration testing.
   */
  public static final LobbyConfiguration INTEGRATION_TEST = new LobbyConfiguration(new MemoryPropertyReader(
      ImmutableMap.of(
          LobbyConfiguration.PropertyKeys.POSTGRES_USER, "postgres",
          LobbyConfiguration.PropertyKeys.POSTGRES_PASSWORD, "postgres")));

  private TestLobbyConfigurations() {}
}
