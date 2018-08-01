package games.strategy.engine.lobby.server;

import java.util.logging.Level;

import games.strategy.engine.config.FilePropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.sound.ClipPlayer;
import lombok.extern.java.Log;

/**
 * A 'main' class to launch the lobby server.
 */
@Log
public final class LobbyRunner {
  private LobbyRunner() {}

  /**
   * Launches a lobby instance.
   * Lobby stays running until the process is killed or the lobby is shutdown.
   */
  public static void main(final String... args) {
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
