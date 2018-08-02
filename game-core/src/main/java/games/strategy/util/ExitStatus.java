package games.strategy.util;

/**
 * A process exit status.
 */
public enum ExitStatus {
  /** The process exited successfully (0). */
  SUCCESS(0),

  /** The process exited due to a failure (1). */
  FAILURE(1);

  private final int status;

  private ExitStatus(final int status) {
    assert status >= 0 : "exit status must be non-negative to avoid signal aliasing";

    this.status = status;
  }

  /**
   * Exits the host process with this status.
   */
  public void exit() {
    System.exit(status);
  }
}
