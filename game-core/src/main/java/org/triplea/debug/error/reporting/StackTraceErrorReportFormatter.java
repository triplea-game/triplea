package org.triplea.debug.error.reporting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.LogRecord;
import javax.annotation.Nullable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.triplea.http.client.error.report.ErrorReportRequest;

/**
 * Combines user description input with data from a {@code LogRecord} to create an {@code
 * ErrorReport} object that we can send to the HTTP server.
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE, onConstructor_ = @VisibleForTesting)
class StackTraceErrorReportFormatter implements BiFunction<String, LogRecord, ErrorReportRequest> {

  private final Supplier<String> versionSupplier;

  StackTraceErrorReportFormatter() {
    this(() -> ClientContext.engineVersion().toString());
  }

  @Override
  public ErrorReportRequest apply(final String userDescription, final LogRecord logRecord) {
    return ErrorReportRequest.builder()
        .title(versionSupplier.get() + ": " + createTitle(logRecord))
        .body(buildBody(userDescription, logRecord))
        .build();
  }

  /**
   * Creates a title, accounting for log message and exception being optional. If so we'll use an
   * empty string to take their place. At a minimum we will always be logging the location of the
   * logging or exception, and we expect for there to always either be at least a log message or an
   * exception.
   */
  private static String createTitle(final LogRecord logRecord) {
    // if we have a stack trace with a triplea class in it, use that for the title;
    // EG: org.triplea.TripleaClass.method:[lineNumber] - [exception]; Caused by: [exception cause]
    return extractTripleAClassAndLine(logRecord)
        .map(
            title ->
                title + " - " + extractExceptionCauseNameOrUseExceptionName(logRecord.getThrown()))
        .orElseGet(
            // Use the log record to create a title.
            () -> {
              final String classNameWithoutPackage =
                  logRecord.getSourceClassName().contains(".")
                      ? logRecord
                          .getSourceClassName()
                          .substring(logRecord.getSourceClassName().lastIndexOf(".") + 1)
                      : logRecord.getSourceClassName();

              return classNameWithoutPackage
                  + "#"
                  + logRecord.getSourceMethodName()
                  + Optional.ofNullable(logRecord.getMessage())
                      .map(message -> " - " + message)
                      .orElse("");
            });
  }

  private static String extractExceptionCauseNameOrUseExceptionName(final Throwable thrown) {
    return Optional.ofNullable(thrown.getCause()) //
        .orElse(thrown)
        .getClass()
        .getSimpleName();
  }

  private static Optional<String> extractTripleAClassAndLine(final LogRecord logRecord) {
    // Try to use any exception cause if we have it otherwise use our exception.
    // Once we have an exception send the stack trace for formatting.
    // If the stack trace contains a triplea class we'll get a formatted message back,
    // otherwise we will get an empty value back.
    return Optional.ofNullable(logRecord.getThrown())
        .map(Throwable::getCause)
        .or(() -> Optional.ofNullable(logRecord.getThrown()))
        .map(Throwable::getStackTrace)
        .map(StackTraceErrorReportFormatter::extractTripleAClassAndLineNumberFromStackTrace);
  }

  /**
   * Searches a stack trace for the first triplea class reported (as identified by package name),
   * using that stack trace element we create a formatted message with exception class name, method
   * name and line number.
   */
  @Nullable
  private static String extractTripleAClassAndLineNumberFromStackTrace(
      final StackTraceElement[] stackTrace) {
    return Arrays.stream(stackTrace)
        .filter(
            stackTraceElement ->
                stackTraceElement.getClassName().contains("triplea")
                    || stackTraceElement.getClassName().contains("games.strategy"))
        .findFirst()
        .map(StackTraceErrorReportFormatter::formatStackTraceElement)
        .orElse(null);
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
  private String buildBody(@Nullable final String userDescription, final LogRecord logRecord) {
    return Optional.ofNullable(Strings.emptyToNull(userDescription))
            .map(description -> "## User Description\n" + description + "\n\n")
            .orElse("")
        + Optional.ofNullable(logRecord.getMessage())
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
        + Optional.ofNullable(logRecord.getThrown())
            .map(StackTraceErrorReportFormatter::throwableToString)
            .orElse("");
  }

  private static String throwableToString(final Throwable e) {
    final var outputStream = new ByteArrayOutputStream();
    try (PrintWriter printWriter = new PrintWriter(outputStream, false, StandardCharsets.UTF_8)) {
      e.printStackTrace(printWriter);
    }
    return "## Exception \n"
        + e.getClass().getName()
        + Optional.ofNullable(e.getMessage()).map(msg -> ": " + msg).orElse("")
        + "\n\n## Stack Trace\n```\n"
        + outputStream.toString(StandardCharsets.UTF_8)
        + "\n```\n\n";
  }
}
