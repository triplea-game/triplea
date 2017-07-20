package games.strategy.persistence.memento.serializable;

import javax.annotation.Nullable;

/**
 * A checked exception that indicates an error occurred while importing a memento in Java object serialization format.
 */
public final class SerializableMementoImportException extends Exception {
  private static final long serialVersionUID = -7212959875208415015L;

  public SerializableMementoImportException(final @Nullable Throwable cause) {
    super(cause);
  }
}
