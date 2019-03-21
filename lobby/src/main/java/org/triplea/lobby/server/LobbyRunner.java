package org.triplea.lobby.server;

import java.util.Optional;
import java.util.logging.Level;

import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.server.ServerConfiguration;
import org.triplea.server.http.spark.SparkServer;
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
    if (Boolean.valueOf(EnvironmentVariable.PROD.getValue())) {
      EnvironmentVariable.verifyProdConfiguration();
    }

    try {
      final LobbyConfiguration lobbyConfiguration = new LobbyConfiguration();
      log.info("Starting lobby socket listener on port " + lobbyConfiguration.getPort());
      LobbyServer.start(lobbyConfiguration);
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to start lobby", e);
      ExitStatus.FAILURE.exit();
    }

    try {
      SparkServer.start(ServerConfiguration.fromEnvironmentVariables());
    } catch (final Error e) {
      log.log(
          Level.SEVERE,
          String.format(
              "Server crash: %s",
              Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse(e.getMessage())));
      ExitStatus.FAILURE.exit();
    }
  }
}
