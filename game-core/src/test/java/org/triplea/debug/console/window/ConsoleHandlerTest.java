package org.triplea.debug.console.window;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class ConsoleHandlerTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class PublishTest {
    @Mock private ConsoleWindow console;
    private ConsoleHandler consoleHandler;
    private final LogRecord record = new LogRecord(Level.SEVERE, "message");

    @BeforeEach
    void createConsoleHandler() {
      consoleHandler = new ConsoleHandler(console);
    }

    @Test
    void shouldAppendFormattedMessageToConsoleWhenRecordIsLoggable(
        @Mock final Formatter formatter) {
      final String formattedMessage = "formattedMessage";
      when(formatter.format(record)).thenReturn(formattedMessage);
      consoleHandler.setFormatter(formatter);

      consoleHandler.publish(record);

      verify(console).append(formattedMessage);
    }

    @Test
    void shouldNotAppendMessageToConsoleWhenRecordIsNotLoggable(@Mock final Filter filter) {
      when(filter.isLoggable(record)).thenReturn(false);
      consoleHandler.setFilter(filter);

      consoleHandler.publish(record);

      verify(console, never()).append(anyString());
    }
  }
}
