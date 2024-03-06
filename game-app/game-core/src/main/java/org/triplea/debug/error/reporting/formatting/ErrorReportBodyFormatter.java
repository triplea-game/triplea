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
   * exception message (but that is okay!). Otherwise, we will log base information like OS, TripleA
   * version and Java version.
   */
  public static String buildBody(
      @Nullable final String userDescription,
      @Nullable final String mapName,
      final LoggerRecord logRecord,
      final Version engineVersion) {
    return Optional.ofNullable(Strings.emptyToNull(userDescription))
            .map(description -> "## User Description\n" + description + "\n\n")
            .orElse("")
        + Optional.ofNullable(Strings.emptyToNull(mapName))
            .map(description -> "## Map\n" + mapName + "\n\n")
            .orElse("")
        + Optional.ofNullable(logRecord.getLogMessage())
            .map(msg -> "## Log Message\n" + msg + "\n\n")
            .orElse("")
        + ("## TripleA Version\n" + linkifyEngineVersion(engineVersion) + "\n\n")
        + ("## Java Version\n" + SystemProperties.getJavaVersion() + "\n\n")
        + ("## Operating System\n" + SystemProperties.getOperatingSystem() + "\n\n")
        + ("## Heap Size\n" + getHeapSize() + "\n\n")
        + (logRecord.getExceptions().isEmpty()
            ? ""
            : "\n\n" + ErrorReportBodyFormatter.throwableToString(logRecord.getExceptions()));
  }

  private String linkifyEngineVersion(Version engineVersion) {
    return String.format(
        "[%s](https://github.com/triplea-game/triplea/releases/tag/%s)",
        engineVersion, engineVersion);
  }

  private String getHeapSize() {
    return Runtime.getRuntime().maxMemory() / 1024 / 1024 + "M";
  }

  private static String throwableToString(final List<ExceptionDetails> exceptionDetails) {
    final String traces =
        exceptionDetails.stream()
            .map(ErrorReportBodyFormatter::formatException)
            .collect(Collectors.joining("\n"));

    return "## Stack Trace\n```\n" + traces + "\n```\n\n";
  }

  private static String formatException(ExceptionDetails e) {
    final var outputStream = new ByteArrayOutputStream();
    if (e.getStackTraceElements() != null && e.getStackTraceElements().length > 0) {
      outputStream.write('\n');
      try (var printWriter = new PrintWriter(outputStream, false, StandardCharsets.UTF_8)) {
        final var exceptionForStackTracePrinting = new Exception();
        exceptionForStackTracePrinting.setStackTrace(e.getStackTraceElements());
        exceptionForStackTracePrinting.printStackTrace(printWriter);
      }
      outputStream.write('\n');
    }
    return "Exception: "
        + e.getExceptionClassName()
        + " "
        + Optional.ofNullable(e.getExceptionMessage()).orElse("")
        + outputStream.toString(StandardCharsets.UTF_8);
  }
}
