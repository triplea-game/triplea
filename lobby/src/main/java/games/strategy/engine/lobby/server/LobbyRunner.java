package games.strategy.engine.lobby.server;

import java.util.logging.Level;
import java.util.logging.Logger;

import games.strategy.engine.config.FilePropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
import games.strategy.sound.ClipPlayer;

/**
 * A 'main' class to launch the lobby server.
 */
public class LobbyRunner {
  private static final Logger logger = Logger.getLogger(LobbyRunner.class.getName());

  /**
   * Launches a lobby instance.
   * Lobby stays running until the process is killed or the lobby is shutdown.
   */
  public static void main(final String[] args) {
    try {
      ClipPlayer.setBeSilentInPreferencesWithoutAffectingCurrent(true);

      final LobbyPropertyReader lobbyPropertyReader =
          new LobbyPropertyReader(new FilePropertyReader("config/lobby/lobby.properties"));
      logger.info("Trying to listen on port " + lobbyPropertyReader.getPort());
      new LobbyServer(lobbyPropertyReader);
      logger.info("Lobby started");
    } catch (final Exception ex) {
      logger.log(Level.SEVERE, ex.toString(), ex);
    }
  }
}
