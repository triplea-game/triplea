package games.strategy.persistence.memento.serializable;

/**
 * A collection of constants for the Java object serialization layer for mementos.
 */
final class SerializableFormatConstants {
  /** The current version of the serializable stream format. */
  static final long CURRENT_VERSION = 1L;

  /** The MIME type of the serializable stream format. */
  static final String MIME_TYPE = "application/x.triplea.memento+serializable";

  private SerializableFormatConstants() {}
}
