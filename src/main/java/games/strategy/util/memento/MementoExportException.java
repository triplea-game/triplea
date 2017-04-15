package games.strategy.util.memento;

/**
 * A checked exception that indicates an error occurred while exporting a memento from its originator.
 *
 * <p>
 * Instances of this class are thread safe.
 * </p>
 */
public final class MementoExportException extends Exception {
  private static final long serialVersionUID = -8515854599628141778L;

  /**
   * Initializes a new instance of the {@code MementoExportException} class with no detail message and no cause.
   */
  public MementoExportException() {}

  /**
   * Initializes a new instance of the {@code MementoExportException} class with the specified detail message and no
   * cause.
   *
   * @param message The detail message; may be {@code null}.
   */
  public MementoExportException(final String message) {
    super(message);
  }

  /**
   * Initializes a new instance of the {@code MementoExportException} class with no detail message and the specified
   * cause.
   *
   * @param cause The cause; may be {@code null}.
   */
  public MementoExportException(final Throwable cause) {
    super(cause);
  }

  /**
   * Initializes a new instance of the {@code MementoExportException} class with the specified detail message and the
   * specified cause.
   *
   * @param message The detail message; may be {@code null}.
   * @param cause The cause; may be {@code null}.
   */
  public MementoExportException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
