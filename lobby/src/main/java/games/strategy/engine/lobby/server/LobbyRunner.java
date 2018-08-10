package games.strategy.engine.lobby.server;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import games.strategy.engine.config.FilePropertyReader;
import games.strategy.engine.config.lobby.LobbyPropertyReader;
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
  public static void main(final String[] args) throws IOException {
    File propertyFile = new File("config/lobby/lobby.properties");
    if (!propertyFile.exists()) {
      log.log(Level.SEVERE, "Could not find property file: " + propertyFile.getAbsolutePath());
      return;
    }

    final LobbyPropertyReader lobbyPropertyReader =
        new LobbyPropertyReader(new FilePropertyReader(propertyFile));
    log.info("Starting lobby on port " + lobbyPropertyReader.getPort());
    LobbyServer.start(lobbyPropertyReader);
    log.info("Lobby started");
  }
}
