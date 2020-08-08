package games.strategy.engine.data;

/** A checked exception that indicates an error occurred while parsing a map. */
public final class GameParseException extends Exception {
  private static final long serialVersionUID = 4015574053053781872L;

  public GameParseException(final String message) {
    super(message);
  }

  public GameParseException(final Throwable cause) {
    super(cause);
  }

  public GameParseException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
