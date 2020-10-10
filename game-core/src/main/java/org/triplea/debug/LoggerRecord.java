package org.triplea.debug;

import java.util.List;

/**
 * Unified interface for data needed to upload error reports.
 *
 * @see LoggerRecordAdapter
 */
public interface LoggerRecord {
  String getLoggerClassName();

  String getLogMessage();

  boolean isError();

  boolean isWarning();

  /**
   * Return the stack trace of each exception, each successive element should be the next 'caused
   * by' exception'.
   */
  List<ExceptionDetails> getExceptions();
}
