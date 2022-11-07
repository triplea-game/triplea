package org.triplea.debug.error.reporting.formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.experimental.UtilityClass;
import org.triplea.debug.ExceptionDetails;
import org.triplea.debug.LoggerRecord;

/** Based on a LogRecord, creates the title of an error report. */
@UtilityClass
public class ErrorReportTitleFormatter {

  /**
   * Creates a title, accounting for log message and exception being optional. If so we'll use an
   * empty string to take their place. At a minimum we will always be logging the location of the
   * logging or exception, and we expect for there to always either be at least a log message or an
   * exception.
   */
  public static String createTitle(final LoggerRecord logRecord) {
    // if we have a stack trace with a triplea class in it, use that for the title;
    // EG: org.triplea.TripleaClass.method:[lineNumber] - [exception]; Caused by: [exception cause]

    final List<StackTraceElement[]> stackTrace =
        logRecord.getExceptions().stream()
            .map(ExceptionDetails::getStackTraceElements)
            .collect(Collectors.toList());

    return extractTripleAClassAndLineNumberFromStackTrace(stackTrace)
        .map(title -> title + " - " + extractExceptionCauseNameOrUseExceptionName(logRecord))
        .orElseGet(
            // Use the log record to create a title.
            () -> {
              final String classNameWithoutPackage =
                  logRecord.getLoggerClassName().contains(".")
                      ? logRecord
                          .getLoggerClassName()
                          .substring(logRecord.getLoggerClassName().lastIndexOf(".") + 1)
                      : logRecord.getLoggerClassName();

              return classNameWithoutPackage
                  + Optional.ofNullable(logRecord.getLogMessage())
                      .map(message -> " - " + message)
                      .orElse("");
            });
  }

  /**
   * Searches a stack trace for the first triplea class reported (as identified by package name),
   * using that stack trace element we create a formatted message with exception class name, method
   * name and line number.
   */
  private static Optional<String> extractTripleAClassAndLineNumberFromStackTrace(
      final List<StackTraceElement[]> stackTrace) {
    if (stackTrace == null || stackTrace.isEmpty()) {
      return Optional.empty();
    }

    return stackTrace.stream()
        .filter(Objects::nonNull)
        .flatMap(Arrays::stream)
        .filter(
            stackTraceElement ->
                stackTraceElement.getClassName().contains("triplea")
                    || stackTraceElement.getClassName().contains("games.strategy"))
        .findFirst()
        .map(ErrorReportTitleFormatter::formatStackTraceElement);
  }

  /**
   * Parses a stack trace element to return a value in this format:
   * [ClassSimpleName]#[MethodName]:[lineNumber]
   */
  @Nullable
  private static String formatStackTraceElement(final StackTraceElement stackTraceElement) {
    final String fileName = stackTraceElement.getFileName();
    if (fileName == null) {
      return null;
    }
    final String fileNameWithoutExtension =
        fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
    return fileNameWithoutExtension
        + "#"
        + stackTraceElement.getMethodName()
        + ":"
        + stackTraceElement.getLineNumber();
  }

  /**
   * Prefer to return the name of the first 'caused by' exception if present, typically it will be
   * more descriptive of the actual error.
   */
  @Nullable
  private static String extractExceptionCauseNameOrUseExceptionName(
      final LoggerRecord loggerRecord) {
    if (loggerRecord.getExceptions().isEmpty()) {
      return null;
    } else if (loggerRecord.getExceptions().size() == 1) {
      return loggerRecord.getExceptions().get(0).getExceptionClassName();
    } else {
      return loggerRecord.getExceptions().get(1).getExceptionClassName();
    }
  }
}
