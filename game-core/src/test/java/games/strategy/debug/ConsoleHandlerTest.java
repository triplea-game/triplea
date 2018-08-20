package games.strategy.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
final class ConsoleHandlerTest {
  private ConsoleHandler consoleHandler;

  @Mock
  private Consumer<LogRecord> errorHandler;

  @BeforeEach
  void setUp() {
    consoleHandler = new ConsoleHandler(errorHandler);
  }

  @Test
  void isLoggable_ShouldReturnFalseWhenRecordIsNull() {
    assertThat(consoleHandler.isLoggable(null), is(false));
  }

  @Test
  void publishWritesToErrorHandler() {
    final LogRecord logRecord = new LogRecord(Level.SEVERE, "message");
    consoleHandler.publish(logRecord);
    verify(errorHandler, times(1)).accept(logRecord);
  }
}
