package org.triplea.debug.error.reporting.formatting;

import com.google.common.base.Strings;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.triplea.debug.ExceptionDetails;
import org.triplea.debug.LoggerRecord;
import org.triplea.util.Version;

/**
 * Based on a LogRecord, creates the body details of an error report. The body is the 'main' part of
 * the error report sent to TripleA and should contain details like the error message and stack
 * trace.
 */
@UtilityClass
public class ErrorReportBodyFormatter {

  /**
   * Creates a string formatted in markdown. If we have an exception we'll log the full exception
   * name and stack trace. If there is a log message we will log it, this might be redundant to the
   * exception message (but that is okay!). Otherwise we will log base information like OS, TripleA
   * version and Java version.
   */
  public static String buildBody(
      @Nullable final String userDescription,
      @Nullable final String mapName,
      final LoggerRecord logRecord,
      final Version engineVersion) {
    String engineVersionLink =
        String.format(
            "[%s](https://github.com/triplea-game/triplea/releases/tag/%s)",
            engineVersion, engineVersion);
    return Optional.ofNullable(Strings.emptyToNull(userDescription))
            .map(description -> "## User Description\n" + description + "\n\n")
            .orElse("")
        + Optional.ofNullable(Strings.emptyToNull(mapName))
            .map(description -> "## Map\n" + mapName + "\n\n")
            .orElse("")
        + Optional.ofNullable(logRecord.getLogMessage())
            .map(msg -> "## Log Message\n" + msg + "\n\n")
            .orElse("")
        + "## TripleA Version\n"
        + engineVersionLink
        + "\n\n"
        + "## Java Version\n"
        + SystemProperties.getJavaVersion()
        + "\n\n"
        + "## Operating System\n"
        + SystemProperties.getOperatingSystem()
        + "\n\n"
        + (logRecord.getExceptions().isEmpty()
            ? ""
            : "\n\n" + ErrorReportBodyFormatter.throwableToString(logRecord.getExceptions()));
  }

  private static String throwableToString(final List<ExceptionDetails> exceptionDetails) {
    final String traces =
        exceptionDetails.stream()
            .map(
                exception -> {
                  if (exception.getStackTraceElements() == null
                      || exception.getStackTraceElements().length == 0) {
                    return "Exception: "
                        + exception.getExceptionClassName()
                        + " "
                        + Optional.ofNullable(exception.getExceptionMessage()).orElse("");
                  } else {
                    final var outputStream = new ByteArrayOutputStream();
                    try (PrintWriter printWriter =
                        new PrintWriter(outputStream, false, StandardCharsets.UTF_8)) {
                      final Exception exceptionForStackTracePrinting = new Exception();
                      exceptionForStackTracePrinting.setStackTrace(
                          exception.getStackTraceElements());
                      exceptionForStackTracePrinting.printStackTrace(printWriter);
                    }
                    return "Exception: "
                        + exception.getExceptionClassName()
                        + " "
                        + Optional.ofNullable(exception.getExceptionMessage()).orElse("")
                        + "\n"
                        + outputStream.toString(StandardCharsets.UTF_8)
                        + "\n";
                  }
                })
            .collect(Collectors.joining("\n"));

    return "## Stack Trace\n```\n" + traces + "\n```\n\n";
  }
}
