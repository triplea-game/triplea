package org.triplea.debug;

import games.strategy.triplea.UrlConstants;
import java.util.function.Function;
import org.triplea.swing.JEditorPaneWithClickableLinks;

/** Converts a 'LogRecord' to the text that we display to a user in an alert pop-up. */
class ErrorMessageFormatter implements Function<LoggerRecord, String> {
  private static final String BREAK = "\n\n";

  @Override
  public String apply(final LoggerRecord logRecord) {
    return "<html>"
        + format(logRecord)
        + (logRecord.isWarning()
            ? "<br><br>If this problem happens frequently and is something you cannot fix,<br>"
                + JEditorPaneWithClickableLinks.toLink(
                    "please report it to TripleA ", UrlConstants.GITHUB_ISSUES)
            : "")
        + "</html>";
  }

  /**
   * Five ways we will hit our logging handler:
   *
   * <pre>
   * (1) uncaught exception with no message, eg: {@code throw new NullPointerException()}
   * (2) uncaught exception with a message, eg:  {@code throw new NullPointerException("message")}
   * (3) logging an error message, eg: {@code log.severe("message")}
   * (4) logging an error message with exception that has no message, eg:
   * {@code log.log(Level.SEVERE, "message", new NullPointerException())}
   * (5) logging an error message with exception that has a message, eg:
   * {@code log.log(Level.SEVERE, "log-message", new NullPointerException("exception message"))}
   * </pre>
   */
  private static String format(final LoggerRecord logRecord) {
    if (logRecord.getLogMessage() == null) {
      final ExceptionDetails exceptionDetails =
          logRecord.getExceptions().size() > 1
              ? logRecord.getExceptions().get(1)
              : logRecord.getExceptions().get(0);

      if (exceptionDetails.getExceptionMessage() != null) {
        return exceptionDetails.getExceptionClassName()
            + " - "
            + exceptionDetails.getExceptionMessage();
      } else {
        return exceptionDetails.getExceptionClassName();
      }
    } else {
      if (logRecord.getExceptions().isEmpty()) {
        return logRecord.getLogMessage();
      } else {
        final ExceptionDetails exceptionDetails =
            logRecord.getExceptions().size() > 1
                ? logRecord.getExceptions().get(1)
                : logRecord.getExceptions().get(0);

        if (exceptionDetails.getExceptionMessage() != null) {
          return logRecord.getLogMessage()
              + BREAK
              + exceptionDetails.getExceptionClassName()
              + ": "
              + exceptionDetails.getExceptionMessage();
        } else {
          return logRecord.getLogMessage() + BREAK + exceptionDetails.getExceptionClassName();
        }
      }
    }
  }
}
