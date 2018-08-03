package tools.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.logging.Level;

import lombok.extern.java.Log;

/**
 * Provides methods for support tools to write log messages.
 */
@Log
public enum ToolLogger {
  ;

  /**
   * Logs the specified error message.
   */
  public static void error(final String message) {
    checkNotNull(message);

    log.log(Level.SEVERE, message);
  }

  /**
   * Logs the specified error message with an associated exception.
   */
  public static void error(final String message, final Throwable t) {
    checkNotNull(message);
    checkNotNull(t);
    log.log(Level.SEVERE, message, t);
  }

  /**
   * Logs the specified informational message.
   */
  public static void info(final String message) {
    checkNotNull(message);
    log.info(message);
  }
}
