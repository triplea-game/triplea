package games.strategy.engine.data;

/** A checked exception that indicates a game engine is not compatible with a map. */
public final class EngineVersionException extends Exception {
  private static final long serialVersionUID = 8800415601463715772L;

  public EngineVersionException(final String error) {
    super(error);
  }
}
