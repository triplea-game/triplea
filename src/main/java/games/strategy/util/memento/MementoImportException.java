package games.strategy.util.memento;

/**
 * A checked exception that indicates an error occurred while importing a memento to its originator.
 *
 * <p>
 * Instances of this class are thread safe.
 * </p>
 */
public final class MementoImportException extends Exception {
  private static final long serialVersionUID = -3885032648502798651L;

  /**
   * Initializes a new instance of the {@code MementoImportException} class with no detail message and no cause.
   */
  public MementoImportException() {}

  /**
   * Initializes a new instance of the {@code MementoImportException} class with the specified detail message and no
   * cause.
   *
   * @param message The detail message; may be {@code null}.
   */
  public MementoImportException(final String message) {
    super(message);
  }

  /**
   * Initializes a new instance of the {@code MementoImportException} class with no detail message and the specified
   * cause.
   *
   * @param cause The cause; may be {@code null}.
   */
  public MementoImportException(final Throwable cause) {
    super(cause);
  }

  /**
   * Initializes a new instance of the {@code MementoImportException} class with the specified detail message and the
   * specified cause.
   *
   * @param message The detail message; may be {@code null}.
   * @param cause The cause; may be {@code null}.
   */
  public MementoImportException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
