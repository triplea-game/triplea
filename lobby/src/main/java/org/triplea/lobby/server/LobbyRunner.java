package org.triplea.lobby.server;

import java.util.logging.Level;

import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.util.ExitStatus;

import lombok.extern.java.Log;

/**
 * Runs a lobby server.
 */
@Log
public final class LobbyRunner {
  private LobbyRunner() {}

  /**
   * Entry point for running a new lobby server. The lobby server runs until the process is killed or the lobby server
   * is shut down via administrative command.
   */
  public static void main(final String[] args) {
    try {
      final LobbyConfiguration lobbyConfiguration = new LobbyConfiguration();
      log.info("Starting lobby socket listener on port " + lobbyConfiguration.getPort());
      LobbyServer.start(lobbyConfiguration);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to start lobby", e);
      ExitStatus.FAILURE.exit();
    }
  }
}
