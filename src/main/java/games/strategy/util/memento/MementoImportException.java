package games.strategy.util.memento;

import javax.annotation.Nullable;

/**
 * A checked exception that indicates an error occurred while importing a memento to its originator.
 */
public final class MementoImportException extends Exception {
  private static final long serialVersionUID = -3885032648502798651L;

  public MementoImportException(final @Nullable String message) {
    super(message);
  }

  public MementoImportException(final @Nullable String message, final @Nullable Throwable cause) {
    super(message, cause);
  }
}
