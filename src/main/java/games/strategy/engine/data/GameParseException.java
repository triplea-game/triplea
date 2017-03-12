package games.strategy.engine.data;

public class GameParseException extends Exception {
  private static final long serialVersionUID = 4015574053053781872L;

  public GameParseException(final String error) {
    super(error);
  }

  public GameParseException(final String mapName, final String error) {
    super("MapName: " + mapName + ", " + error);
  }

}
