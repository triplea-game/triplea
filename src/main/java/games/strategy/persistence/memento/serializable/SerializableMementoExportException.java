package games.strategy.persistence.memento.serializable;

/**
 * A checked exception that indicates an error occurred while exporting a memento in Java object serialization format.
 *
 * <p>
 * Instances of this class are thread safe.
 * </p>
 */
public final class SerializableMementoExportException extends Exception {
  private static final long serialVersionUID = -4053920530609554772L;

  /**
   * Initializes a new instance of the {@code SerializableMementoExportException} class with no detail message and no
   * cause.
   */
  public SerializableMementoExportException() {}

  /**
   * Initializes a new instance of the {@code SerializableMementoExportException} class with the specified detail
   * message and no cause.
   *
   * @param message The detail message; may be {@code null}.
   */
  public SerializableMementoExportException(final String message) {
    super(message);
  }

  /**
   * Initializes a new instance of the {@code SerializableMementoExportException} class with no detail message and the
   * specified cause.
   *
   * @param cause The cause; may be {@code null}.
   */
  public SerializableMementoExportException(final Throwable cause) {
    super(cause);
  }

  /**
   * Initializes a new instance of the {@code SerializableMementoExportException} class with the specified detail
   * message and the specified cause.
   *
   * @param message The detail message; may be {@code null}.
   * @param cause The cause; may be {@code null}.
   */
  public SerializableMementoExportException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
