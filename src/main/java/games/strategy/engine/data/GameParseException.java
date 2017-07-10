package games.strategy.engine.data;

public class GameParseException extends Exception {
  private static final long serialVersionUID = 4015574053053781872L;

  public GameParseException(final String error) {
    super(error);
  }

  public GameParseException(final String mapName, final String error) {
    this("MapName: " + mapName + ", " + error);
  }

  public GameParseException(final String mapName, final String error, final Throwable cause) {
    this("MapName: " + mapName + ", " + error, cause);
  }

  public GameParseException(final String message, final Throwable cause) {
    super(message, cause);
  }

}
