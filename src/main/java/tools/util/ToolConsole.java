package tools.util;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A collection of methods for use by support tools to write messages to the console.
 */
public final class ToolConsole {
  private ToolConsole() {}

  /**
   * Writes the specified error message to the console.
   */
  public static void error(final String message) {
    checkNotNull(message);

    System.err.println(message);
  }

  /**
   * Writes the specified error message and exception to the console.
   */
  public static void error(final String message, final Throwable t) {
    checkNotNull(message);
    checkNotNull(t);

    error(message);
    t.printStackTrace(System.err);
  }

  /**
   * Writes the specified informational message to the console.
   */
  public static void info(final String message) {
    checkNotNull(message);

    System.out.println(message);
  }
}
