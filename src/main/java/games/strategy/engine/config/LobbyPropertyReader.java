package games.strategy.engine.config;

/**
 * Reads property values from the lobby configuration file.
 */
public class LobbyPropertyReader extends PropertyFileReader {
  private static final String LOBBY_PROPERTIES_FILE = "lobby.properties";

  public LobbyPropertyReader() {
    super(LOBBY_PROPERTIES_FILE);
  }

  public int getPort() {
    return Integer.parseInt(readProperty("port"));
  }
}
