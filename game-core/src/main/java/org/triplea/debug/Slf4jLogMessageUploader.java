package org.triplea.debug;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * This appender is activated via logback.xml and can accept SLF4J logger messages. Log messages are
 * displayed as an error dialog to users with an option for 'error' logs to be uploaded
 * automatically to github issues.
 */
public class Slf4jLogMessageUploader extends AppenderBase<ILoggingEvent> {
  @Override
  protected void append(final ILoggingEvent eventObject) {
    ErrorMessage.show(LoggerRecordAdapter.fromLogbackEvent(eventObject));
  }
}
