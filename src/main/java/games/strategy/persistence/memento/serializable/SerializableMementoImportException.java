package games.strategy.persistence.memento.serializable;

/**
 * A checked exception that indicates an error occurred while importing a memento in Java object serialization format.
 *
 * <p>
 * Instances of this class are thread safe.
 * </p>
 */
public final class SerializableMementoImportException extends Exception {
  private static final long serialVersionUID = -7212959875208415015L;

  /**
   * Initializes a new instance of the {@code SerializableMementoImportException} class with no detail message and no
   * cause.
   */
  public SerializableMementoImportException() {}

  /**
   * Initializes a new instance of the {@code SerializableMementoImportException} class with the specified detail
   * message and no cause.
   *
   * @param message The detail message; may be {@code null}.
   */
  public SerializableMementoImportException(final String message) {
    super(message);
  }

  /**
   * Initializes a new instance of the {@code SerializableMementoImportException} class with no detail message and the
   * specified cause.
   *
   * @param cause The cause; may be {@code null}.
   */
  public SerializableMementoImportException(final Throwable cause) {
    super(cause);
  }

  /**
   * Initializes a new instance of the {@code SerializableMementoImportException} class with the specified detail
   * message and the specified cause.
   *
   * @param message The detail message; may be {@code null}.
   * @param cause The cause; may be {@code null}.
   */
  public SerializableMementoImportException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
