package org.triplea.debug;

import games.strategy.triplea.UrlConstants;
import java.util.Optional;
import java.util.function.Function;
import org.triplea.swing.JEditorPaneWithClickableLinks;

/** Converts a 'LogRecord' to the text that we display to a user in an alert pop-up. */
class ErrorMessageFormatter implements Function<LoggerRecord, String> {
  static final String UNEXPECTED_ERROR_TEXT = "An unexpected error occurred!";

  @Override
  public String apply(final LoggerRecord logRecord) {
    final String result =
        "<html>"
            + format(logRecord)
            + (logRecord.isWarning()
                ? "<br><br>If this problem happens frequently and is something you cannot fix,<br>"
                    + JEditorPaneWithClickableLinks.toLink(
                        "please report it to TripleA ", UrlConstants.GITHUB_ISSUES)
                : "")
            + "</html>";

    return result.replaceAll("\n", "<br/>");
  }

  /**
   * Creates a message based on a log record with this format:
   *
   * <pre>
   *   {log message}
   *   {exception 1 class name}: {exception 1 message}
   *   {exception 2 class name}: {exception 2 message}
   *   {exception 3 class name}: {exception 3 message}
   * </pre>
   *
   * We will not print an exception message line if the message is the same as the logger message.
   * We will also only print up to 3 additional exception messages.
   *
   * <p>There are five ways we will hit our logging handler:
   *
   * <pre>
   * (1) uncaught exception with no message, eg: {@code throw new NullPointerException()}
   * (2) uncaught exception with a message, eg:  {@code throw new NullPointerException("message")}
   * (3) logging an error message, eg: {@code log.error("message")}
   * (4) logging an error message with exception that has no message, eg:
   * {@code log.error("message", new NullPointerException())}
   * (5) logging an error message with exception that has a message, eg:
   * {@code log.error("log-message", new NullPointerException("exception message"))}
   * </pre>
   */
  private static String format(final LoggerRecord logRecord) {
    StringBuilder errorMessage = new StringBuilder();

    // If there is no log message, or if the log message matches the error message
    // of the first exception, then our error message header will be 'unexpected error'.
    // Otherwise the error header is the (unique and non-null) log message.
    if (logRecord.getLogMessage() == null
        || (!logRecord.getExceptions().isEmpty()
            && logRecord
                .getLogMessage()
                .equals(logRecord.getExceptions().get(0).getExceptionMessage()))) {
      errorMessage.append("<b>").append(UNEXPECTED_ERROR_TEXT).append("</b>");
    } else {
      errorMessage.append("<b>").append(logRecord.getLogMessage()).append("</b>");
    }

    // Print nested exceptions
    for (int i = 0; i < logRecord.getExceptions().size() && i < 3; i++) {
      ExceptionDetails exceptionDetails = logRecord.getExceptions().get(i);

      errorMessage
          .append("\n\n")
          .append(formatExceptionClassName(exceptionDetails.getExceptionClassName()));

      Optional.ofNullable(exceptionDetails.getExceptionMessage())
          .ifPresent(exceptionMessage -> errorMessage.append(": ").append(exceptionMessage));
    }

    return errorMessage.toString();
  }

  /**
   * Remove package names from a class name, eg: 'java.lang.RuntimeExcception' -> 'RuntimeException'
   */
  private static String formatExceptionClassName(String className) {
    return className.contains(".")
        ? className.substring(className.lastIndexOf(".") + 1)
        : className;
  }
}
