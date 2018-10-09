package org.triplea.http.client.error.report.create;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.annotation.Nullable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Represents data that would be uploaded to a server.
 */
@Getter
@ToString
@EqualsAndHashCode
public class ErrorReport {

  private final String title;
  @Nullable
  private final String problemDescription;
  private final String gameVersion;
  private final String operatingSystem;
  private final String javaVersion;

  private final String logLevel;
  private final String errorMessageToUser;
  private final String exceptionMessage;
  private final String stackTrace;
  private final String className;
  private final String methodName;

  public ErrorReport(final ErrorReportDetails details) {
    this.title = details.getTitle();
    this.problemDescription = details.getProblemDescription();
    this.gameVersion = details.getGameVersion();

    operatingSystem = System.getProperty("os.name");
    javaVersion = System.getProperty("java.version");
    logLevel = details.getLogRecord()
        .map(LogRecord::getLevel)
        .map(Level::toString)
        .orElse("");
    errorMessageToUser = details.getLogRecord()
        .map(LogRecord::getMessage)
        .orElse("");
    exceptionMessage = details.getLogRecord()
        .map(LogRecord::getThrown)
        .map(Throwable::getMessage)
        .orElse("");
    stackTrace = details.getLogRecord()
        .map(LogRecord::getThrown)
        .map(Throwable::getStackTrace)
        .map(Arrays::toString)
        .orElse("");
    className = details.getLogRecord()
        .map(LogRecord::getSourceClassName)
        .orElse("");
    methodName = details.getLogRecord()
        .map(LogRecord::getSourceMethodName)
        .orElse("");
  }

}
