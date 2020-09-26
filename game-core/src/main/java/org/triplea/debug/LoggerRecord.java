package org.triplea.debug;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Getter;

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

  @Builder
  @Getter
  class ExceptionDetails {
    @Nonnull private final String exceptionClassName;
    @Nullable private final String exceptionMessage;
    @Nullable private final StackTraceElement[] stackTraceElements;
  }
}
