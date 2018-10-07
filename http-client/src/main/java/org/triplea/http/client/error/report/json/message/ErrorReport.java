package org.triplea.http.client.error.report.json.message;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import lombok.Getter;
import lombok.ToString;

/**
 * Represents data that would be uploaded to a server.
 */
@Getter
@ToString
// TODO: rename to ErrorReportRequest
// TODO: merge 'ErrorReportDetails' into this class
public class ErrorReport {

  private final String messageFromUser;
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
    final String messageFromUser = details.getMessageFromUser();
    final String gameVersion = details.getGameVersion();

    this.messageFromUser = messageFromUser;
    this.gameVersion = gameVersion;
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
