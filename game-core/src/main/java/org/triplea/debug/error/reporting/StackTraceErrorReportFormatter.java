package org.triplea.debug.error.reporting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.system.SystemProperties;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
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

    // an exception is optional, eg: 'log.severe("message only, no exception")'
    final String exceptionNamePrefix =
        Optional.ofNullable(logRecord.getThrown())
            .map(Throwable::getClass)
            .map(Class::getSimpleName)
            .map(simpleName -> simpleName + " - ")
            .orElse("");

    final String classShortName =
        logRecord.getSourceClassName().contains(".")
            ? logRecord
                .getSourceClassName()
                .substring(logRecord.getSourceClassName().lastIndexOf('.') + 1)
            : logRecord.getSourceClassName();

    final String sourceLocation = classShortName + "." + logRecord.getSourceMethodName();

    // for title prefer to use the exception message when we have it, if not fall back to the log
    // message
    final String message =
        Optional.ofNullable(
                Optional.ofNullable(logRecord.getThrown())
                    .map(Throwable::getMessage)
                    .orElse(logRecord.getMessage()))
            .map(m -> ": " + m)
            .orElse("");

    return exceptionNamePrefix + sourceLocation + message;
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
