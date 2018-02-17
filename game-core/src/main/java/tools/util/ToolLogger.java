package tools.util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides methods for support tools to write log messages.
 */
public final class ToolLogger {
  private ToolLogger() {}

  /**
   * Logs the specified error message.
   */
  public static void error(final String message) {
    checkNotNull(message);

    System.err.println(message);
  }

  /**
   * Logs the specified error message with an associated exception.
   */
  public static void error(final String message, final Throwable t) {
    checkNotNull(message);
    checkNotNull(t);

    error(message);
    t.printStackTrace(System.err);
  }

  /**
   * Logs the specified informational message.
   */
  public static void info(final String message) {
    checkNotNull(message);

    System.out.println(message);
  }
}
