package org.triplea.debug;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.experimental.UtilityClass;

/**
 * Contains adapter methods to convert log events from either java.util.logger or logback to a
 * unified @{code LoggerRecord}. The unified data is then used to create error reports that can be
 * uploaded.
 */
@UtilityClass
class LoggerRecordAdapter {

  static LoggerRecord fromLogbackEvent(final ILoggingEvent eventObject) {
    return new LoggerRecord() {
      @Override
      public String getLoggerClassName() {
        return eventObject.getLoggerName();
      }

      @Override
      public String getLogMessage() {
        return eventObject.getFormattedMessage();
      }

      @Override
      public boolean isError() {
        return eventObject.getLevel().isGreaterOrEqual(Level.ERROR);
      }

      @Override
      public boolean isWarning() {
        return eventObject.getLevel() == Level.WARN;
      }

      @Override
      public List<ExceptionDetails> getExceptions() {
        final List<ExceptionDetails> details = new ArrayList<>();
        IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
        // get up 20 exceptions via 'caused by' and convert each to exception details
        for (int i = 0; i < 20 && throwableProxy != null; i++) {
          details.add(toExceptionDetails(throwableProxy));
          throwableProxy = throwableProxy.getCause();
        }
        return details;
      }

      private ExceptionDetails toExceptionDetails(final IThrowableProxy throwableProxy) {
        return ExceptionDetails.builder()
            .exceptionClassName(throwableProxy.getClassName())
            .exceptionMessage(throwableProxy.getMessage())
            .stackTraceElements(mapThrowableProxyToStackTrace(throwableProxy))
            .build();
      }

      private StackTraceElement[] mapThrowableProxyToStackTrace(final IThrowableProxy proxy) {
        return Optional.ofNullable(proxy)
            .map(IThrowableProxy::getStackTraceElementProxyArray)
            .map(this::mapProxyArrayToStackTraceArray)
            .orElse(null);
      }

      private StackTraceElement[] mapProxyArrayToStackTraceArray(
          final StackTraceElementProxy[] proxies) {
        return Arrays.stream(proxies)
            .map(StackTraceElementProxy::getStackTraceElement)
            .toArray(StackTraceElement[]::new);
      }
    };
  }
}
