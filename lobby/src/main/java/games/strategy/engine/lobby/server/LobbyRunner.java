package games.strategy.engine.lobby.server;

import java.util.logging.Level;

import games.strategy.engine.config.FilePropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.sound.ClipPlayer;
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
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);

      final LobbyPropertyReader lobbyPropertyReader =
          new LobbyPropertyReader(new FilePropertyReader("config/lobby/lobby.properties"));
      log.info("Starting lobby on port " + lobbyPropertyReader.getPort());
      new LobbyServer(lobbyPropertyReader);
      log.info("Lobby started");
    } catch (final RuntimeException e) {
      log.log(Level.SEVERE, "Failed to start lobby", e);
      System.exit(1);
    }
  }
}
