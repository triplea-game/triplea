package games.strategy.persistence.memento.serializable;

import javax.annotation.Nullable;

/**
 * A checked exception that indicates an error occurred while exporting a memento in Java object serialization format.
 */
public final class SerializableMementoExportException extends Exception {
  private static final long serialVersionUID = -4053920530609554772L;

  public SerializableMementoExportException(final @Nullable Throwable cause) {
    super(cause);
  }
}
