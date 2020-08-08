package org.triplea.debug;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class ErrorMessageHandlerTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class PublishTest {
    @Mock private Consumer<LogRecord> consumer;
    private final ErrorMessageHandler errorMessageHandler = new ErrorMessageHandler();

    private LogRecord newLogRecord(final int levelValue) {
      final Level level =
          new Level("name", levelValue) {
            private static final long serialVersionUID = 1L;
          };
      return new LogRecord(level, "message");
    }

    @Test
    void shouldInvokeConsumerWhenRecordIsLoggableAndLogLevelIsEqualToThreshold() {
      final LogRecord record = newLogRecord(ErrorMessageHandler.THRESHOLD_LEVEL_VALUE);

      errorMessageHandler.publish(record, consumer);

      verify(consumer).accept(record);
    }

    @Test
    void shouldInvokeConsumerWhenRecordIsLoggableAndLogLevelIsGreaterThanThreshold() {
      final LogRecord record = newLogRecord(ErrorMessageHandler.THRESHOLD_LEVEL_VALUE + 1);

      errorMessageHandler.publish(record, consumer);

      verify(consumer).accept(record);
    }

    @Test
    void shouldNotInvokeConsumerWhenRecordIsNotLoggable(@Mock final Filter filter) {
      final LogRecord record = newLogRecord(ErrorMessageHandler.THRESHOLD_LEVEL_VALUE);
      when(filter.isLoggable(record)).thenReturn(false);
      errorMessageHandler.setFilter(filter);

      errorMessageHandler.publish(record, consumer);

      verify(consumer, never()).accept(any());
    }

    @Test
    void shouldNotInvokeConsumerWhenLogLevelIsBelowThreshold() {
      final LogRecord record = newLogRecord(ErrorMessageHandler.THRESHOLD_LEVEL_VALUE - 1);

      errorMessageHandler.publish(record, consumer);

      verify(consumer, never()).accept(any());
    }
  }
}
