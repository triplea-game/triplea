package org.triplea.debug;

import static com.google.common.base.Preconditions.checkArgument;

import games.strategy.triplea.UrlConstants;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.triplea.swing.JEditorPaneWithClickableLinks;

/** Converts a 'LogRecord' to the text that we display to a user in an alert pop-up. */
class ErrorMessageFormatter implements Function<LogRecord, String> {
  private static final String BREAK = "\n\n";

  @Override
  public String apply(final LogRecord logRecord) {
    checkArgument(
        logRecord.getThrown() != null || logRecord.getMessage() != null,
        "LogRecord should have one or both, a message or exception: " + logRecord);

    final String baseMessage = format(logRecord);

    final String additionalMessageForWarnings =
        (logRecord.getLevel().intValue() == Level.WARNING.intValue())
            ? "<br><br>If this problem happens frequently and is something you cannot fix,<br>"
                + JEditorPaneWithClickableLinks.toLink(
                    "please report it to TripleA ", UrlConstants.GITHUB_ISSUES)
            : "";
    return TextUtils.textToHtml(baseMessage + additionalMessageForWarnings);
  }

  /**
   * Five ways we will hit our logging handler.:<br>
   * (1) uncaught exception with no message {@code throw new NullPointerException() =>
   * LogRecord.getMessage() == null && LogRecord.getThrown().getMessage() == null } Return: *
   * {exception simple name} <br>
   * (2) uncaught exception with a message {@code throw new NullPointerException("message") => //
   * log record message will be populated with the exception message
   * LogRecord.getMessage().equals(LogRecord.getThrown().getMessage())} } <br>
   * Return: * {exception simple name} - {exception message} <br>
   * (3) logging an error message {@code log.severe("message") => LogRecord.getMessage() != null &&
   * LogRecord.getThrown() == null } <br>
   * Return: * {log message} <br>
   * (4) logging an error message with exception that has no message {@code log.log(Level.SEVERE,
   * "message", new NullPointerException()) => LogRecord.getMessage() != null &&
   * LogRecord.getThrown().getMessage() == null } Return: * * {log message} * * * * {exception
   * simple name} <br>
   * (5) logging an error message with exception that has a message {@code log.log(Level.SEVERE,
   * "log-message", new NullPointerException("exception message")) => LogRecord.getMessage() != null
   * && LogRecord.getThrown() != null &&
   * !LogRecord.getMessage().equals(LogRecord.getThrown().getMessage()) } <br>
   * Return: * {log message} * * {exception simple name} - {exception message}
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

    throw new IllegalStateException(
        "Unhandled: " + logRecord.getMessage() + ", exception: " + logRecord.getThrown());
  }

  /*
   * <pre>
   * Error: {log message}
   * </pre>
   */
  private static String logMessageOnly(final LogRecord logRecord) {
    checkArgument(logRecord.getMessage() != null);
    checkArgument(logRecord.getThrown() == null);

    return logRecord.getMessage();
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
    return simpleName(logRecord) + " - " + logRecord.getThrown().getMessage();
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

    return simpleName(logRecord);
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

    return logRecord.getMessage()
        + BREAK
        + simpleName(logRecord)
        + ": "
        + logRecord.getThrown().getMessage();
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

    return logRecord.getMessage() + BREAK + simpleName(logRecord);
  }
}
