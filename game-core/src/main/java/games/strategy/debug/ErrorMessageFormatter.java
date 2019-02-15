package games.strategy.debug;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.function.Function;
import java.util.logging.LogRecord;

/**
 * Converts a 'LogRecord' to the text that we display to a user in an alert pop-up.
 */
class ErrorMessageFormatter implements Function<LogRecord, String> {
  private static final String ERROR_LINE_PREFIX = "Error: ";
  private static final String LOCATION_LINE_PREFIX = "Location: ";
  private static final String DETAILS_LINE_PREFIX = "Details: ";

  private static final String BREAK = "\n\n";

  @Override
  public String apply(final LogRecord logRecord) {
    checkArgument(logRecord.getThrown() != null || logRecord.getMessage() != null,
        "LogRecord should have one or both, a message or exception: " + logRecord);

    return TextUtils.textToHtml(format(logRecord) + BREAK + locationLine(logRecord));
  }

  /**
   * Five ways we will hit our logging handler.:<br/>
   * (1) uncaught exception with no message
   * {@code throw new NullPointerException() =>
   *      LogRecord.getMessage() == null && LogRecord.getThrown().getMessage() == null
   * }
   * Return:
   * * Error: {exception simple name}
   * *
   * * Location: {className}.{methodName}
   * <br/>
   * (2) uncaught exception with a message
   * {@code
   *         throw new NullPointerException("message") =>
   *              // log record message will be populated with the exception message
   *              LogRecord.getMessage().equals(LogRecord.getThrown().getMessage())}
   * }
   * <br/>
   * Return:
   * * Error: {exception simple name} - {exception message}
   * *
   * * Location: {className}.{methodName}
   * <br/>
   * (3) logging an error message
   * {@code
   *         log.severe("message") =>
   *              LogRecord.getMessage() != null && LogRecord.getThrown() == null
   * }
   * <br/>
   * Return:
   * * Error: {log message}
   * *
   * * Location: {className}.{methodName}
   * <br/>
   * (4) logging an error message with exception that has no message</li>
   * {@code
   *        log.log(Level.SEVERE, "message", new NullPointerException()) =>
   *              LogRecord.getMessage() != null && LogRecord.getThrown().getMessage() == null
   * }
   * Return:
   * * * Error: {log message}
   * * *
   * * * Details: {exception simple name}
   * * *
   * * * Location: {className}.{methodName}
   * <br/>
   * (5) logging an error message with exception that has a message
   * {@code
   *        log.log(Level.SEVERE, "log-message", new NullPointerException("exception message")) =>
   *              LogRecord.getMessage() != null
   *                 && LogRecord.getThrown() != null
   *                 && !LogRecord.getMessage().equals(LogRecord.getThrown().getMessage())
   * }
   * <br/>
   * Return:
   * * Error: {log message}
   * *
   * * Details: {exception simple name} - {exception message}
   * *
   * * Location: {className}.{methodName}
   */
  private static String format(final LogRecord logRecord) {
    if (logRecord.getThrown() == null) {
      return logMessageOnly(logRecord);
    }

    if (logRecord.getMessage() != null
        && logRecord.getThrown() != null
        && logRecord.getThrown().getMessage() != null
        && !logRecord.getMessage().equals(logRecord.getThrown().getMessage())) {
      return logMessageAndExceptionMessage(logRecord);
    }

    if (logRecord.getMessage() != null
        && logRecord.getThrown() != null
        && logRecord.getThrown().getMessage() == null) {
      return logMessageAndExceptionWithoutMessage(logRecord);
    }

    if (logRecord.getMessage() == null
        && logRecord.getThrown() != null
        && logRecord.getThrown().getMessage() == null) {
      return exceptionOnlyWithOutMessage(logRecord);
    }

    if (logRecord.getMessage() != null
        && logRecord.getThrown() != null
        && logRecord.getThrown().getMessage() != null
        && logRecord.getMessage().equals(logRecord.getThrown().getMessage())) {
      return exceptionOnlyWithMessage(logRecord);
    }


    throw new IllegalStateException("Unhandled: " + logRecord.getMessage() + ", exception: " + logRecord.getThrown());
  }

  /*
   * <pre>
   * Error: {log message}
   * </pre>
   */
  private static String logMessageOnly(final LogRecord logRecord) {
    checkArgument(logRecord.getMessage() != null);
    checkArgument(logRecord.getThrown() == null);

    return ERROR_LINE_PREFIX + logRecord.getMessage();
  }


  /*
   * <pre>
   * Error: {exception simple name} - {exception message}
   * </pre>
   */
  private static String exceptionOnlyWithMessage(final LogRecord logRecord) {
    checkArgument(logRecord.getThrown().getMessage() != null);
    checkArgument(logRecord.getMessage() != null);
    checkArgument(logRecord.getThrown().getMessage().equals(logRecord.getMessage()));
    return ERROR_LINE_PREFIX + simpleName(logRecord) + " - " + logRecord.getThrown().getMessage();
  }


  private static String simpleName(final LogRecord logRecord) {
    return logRecord.getThrown().getClass().getSimpleName();
  }

  /*
   * <pre>
   * Error: {exception simple name}
   * </pre>
   */
  private static String exceptionOnlyWithOutMessage(final LogRecord logRecord) {
    checkArgument(logRecord.getThrown().getMessage() == null);
    checkArgument(logRecord.getMessage() == null);

    return ERROR_LINE_PREFIX + simpleName(logRecord);
  }

  /*
   * <pre>
   * Error: {log message}
   *
   * Details: {exception simple name} - {exception message}
   * </pre>
   */
  private static String logMessageAndExceptionMessage(final LogRecord logRecord) {
    checkArgument(logRecord.getThrown().getMessage() != null);
    checkArgument(logRecord.getMessage() != null);
    checkArgument(!logRecord.getThrown().getMessage().equals(logRecord.getMessage()));

    return ERROR_LINE_PREFIX + logRecord.getMessage() + BREAK
        + DETAILS_LINE_PREFIX + simpleName(logRecord) + " - " + logRecord.getThrown().getMessage();
  }


  /*
   * <pre>
   * Error: {log message}
   *
   * Details: {exception simple name}
   * </pre>
   */
  private static String logMessageAndExceptionWithoutMessage(final LogRecord logRecord) {
    checkArgument(logRecord.getThrown().getMessage() == null);
    checkArgument(logRecord.getMessage() != null);

    return ERROR_LINE_PREFIX + logRecord.getMessage() + BREAK
        + DETAILS_LINE_PREFIX + simpleName(logRecord);
  }

  /*
   * <pre>
   * Location: {class name}.{method name}
   * </pre>
   */
  private static String locationLine(final LogRecord logRecord) {
    return LOCATION_LINE_PREFIX + logRecord.getSourceClassName() + "." + logRecord.getSourceMethodName();
  }
}
