package games.strategy.engine.data;

import org.xml.sax.SAXException;

public class GameParseException extends SAXException {
  private static final long serialVersionUID = 4015574053053781872L;

  public GameParseException(final String error) {
    super(error);
  }

  public GameParseException(final String mapName, final String error) {
    this("MapName: " + mapName + ", " + error);
  }

  public GameParseException(final String mapName, final String error, final Exception cause) {
    this("MapName: " + mapName + ", " + error, cause);
  }

  public GameParseException(final String message, final Exception cause) {
    super(message, cause);
  }

}
