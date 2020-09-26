package org.triplea.debug.error.reporting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.debug.LoggerRecord;
import org.triplea.debug.LoggerRecord.ExceptionDetails;
import org.triplea.http.client.error.report.ErrorReportRequest;

/**
 * Combines user description input with data from a {@code LogRecord} to create an {@code
 * ErrorReport} object that we can send to the HTTP server.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
class StackTraceErrorReportFormatter
    implements Function<ErrorReportRequestParams, ErrorReportRequest> {

  private final Supplier<String> versionSupplier;

  StackTraceErrorReportFormatter() {
    this(() -> ClientContext.engineVersion().toString());
  }

  @Override
  public ErrorReportRequest apply(final ErrorReportRequestParams errorReportRequestParams) {
    return ErrorReportRequest.builder()
        .title(createTitle(errorReportRequestParams.getLogRecord()))
        .body(
            buildBody(
                errorReportRequestParams.getUserDescription(),
                errorReportRequestParams.getMapName(),
                errorReportRequestParams.getMemoryStatistics(),
                errorReportRequestParams.getLogRecord()))
        .gameVersion(versionSupplier.get())
        .build();
  }

  /**
   * Creates a title, accounting for log message and exception being optional. If so we'll use an
   * empty string to take their place. At a minimum we will always be logging the location of the
   * logging or exception, and we expect for there to always either be at least a log message or an
   * exception.
   */
  static String createTitle(final LoggerRecord logRecord) {
    // if we have a stack trace with a triplea class in it, use that for the title;
    // EG: org.triplea.TripleaClass.method:[lineNumber] - [exception]; Caused by: [exception cause]

    final List<StackTraceElement[]> stackTrace =
        logRecord.getExceptions().stream()
            .map(ExceptionDetails::getStackTraceElements)
            .collect(Collectors.toList());

    return StackTraceErrorReportFormatter.extractTripleAClassAndLineNumberFromStackTrace(stackTrace)
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
        .map(StackTraceErrorReportFormatter::formatStackTraceElement);
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
   * Creates a string formatted in markdown. If we have an exception we'll log the full exception
   * name and stack trace. If there is a log message we will log it, this might be redundant to the
   * exception message (but that is okay!). Otherwise we will log base information like OS, TripleA
   * version and Java version.
   */
  private String buildBody(
      @Nullable final String userDescription,
      @Nullable final String mapName,
      final String memoryStatistics,
      final LoggerRecord logRecord) {
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
        + ClientContext.engineVersion()
        + "\n\n"
        + "## Java Version\n"
        + SystemProperties.getJavaVersion()
        + "\n\n"
        + "## Operating System\n"
        + SystemProperties.getOperatingSystem()
        + "\n\n"
        + "## Memory\n"
        + memoryStatistics
        + (logRecord.getExceptions().isEmpty()
            ? ""
            : "\n\n" + StackTraceErrorReportFormatter.throwableToString(logRecord.getExceptions()));
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
