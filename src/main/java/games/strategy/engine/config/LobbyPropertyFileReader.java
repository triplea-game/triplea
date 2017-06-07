package games.strategy.engine.config;

/**
 * Reads property values from the lobby configuration file.
 */
public class LobbyPropertyFileReader extends PropertyFileReader {
  private static final String LOBBY_PROPERTIES_FILE = "lobby.properties";

  public LobbyPropertyFileReader() {
    super(LOBBY_PROPERTIES_FILE);
  }
}
