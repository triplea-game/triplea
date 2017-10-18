package games.strategy.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintStream;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
public final class ConsoleHandlerTest {
  private ConsoleHandler consoleHandler;

  @Mock
  private PrintStream err;

  @Mock
  private ErrorManager errorManager;

  @Mock
  private Formatter formatter;

  @Mock
  private PrintStream out;

  @BeforeEach
  public void setUp() {
    consoleHandler = new ConsoleHandler(() -> out, () -> err);
    consoleHandler.setErrorManager(errorManager);
  }

  @Test
  public void close_ShouldFlushButNotCloseStreams() {
    consoleHandler.close();

    verify(err).flush();
    verify(err, never()).close();
    verify(out).flush();
    verify(out, never()).close();
  }

  @Test
  public void flush_ShouldFlushStreams() {
    consoleHandler.flush();

    verify(err).flush();
    verify(out).flush();
  }

  @Test
  public void isLoggable_ShouldReturnFalseWhenRecordIsNull() {
    assertThat(consoleHandler.isLoggable(null), is(false));
  }

  @Test
  public void publish_ShouldWriteToErrStreamWhenLevelIsWarningOrGreater() {
    consoleHandler.publish(newLogRecordForLevel(Level.SEVERE));
    consoleHandler.publish(newLogRecordForLevel(Level.WARNING));

    verify(err, times(2)).print(anyString());
    verify(out, never()).print(anyString());
  }

  private static LogRecord newLogRecordForLevel(final Level level) {
    return new LogRecord(level, "message");
  }

  @Test
  public void publish_ShouldWriteToOutStreamWhenLevelIsInfoOrLower() {
    consoleHandler.publish(newLogRecordForLevel(Level.INFO));
    consoleHandler.publish(newLogRecordForLevel(Level.CONFIG));
    consoleHandler.publish(newLogRecordForLevel(Level.FINE));
    consoleHandler.publish(newLogRecordForLevel(Level.FINER));
    consoleHandler.publish(newLogRecordForLevel(Level.FINEST));

    verify(out, times(5)).print(anyString());
    verify(err, never()).print(anyString());
  }

  @Test
  public void publish_ShouldDoNothingWhenLogRecordIsNotLoggable() {
    consoleHandler.publish(null);

    verify(err, never()).print(anyString());
    verify(out, never()).print(anyString());
    verify(errorManager, never()).error(any(), any(), anyInt());
  }

  @Test
  public void publish_ShouldWriteMessageSuppliedByFormatter() {
    final String message = "the message";
    when(formatter.format(any(LogRecord.class))).thenReturn(message);
    consoleHandler.setFormatter(formatter);

    consoleHandler.publish(newLogRecordForLevel(Level.INFO));

    verify(out).print(message);
  }

  @Test
  public void publish_ShouldReportErrorWhenFormatterThrowsException() {
    final Exception exception = new RuntimeException();
    when(formatter.format(any(LogRecord.class))).thenThrow(exception);
    consoleHandler.setFormatter(formatter);

    consoleHandler.publish(newLogRecordForLevel(Level.INFO));

    verify(errorManager).error(any(), eq(exception), eq(ErrorManager.FORMAT_FAILURE));
    verify(out, never()).print(nullable(String.class));
  }

  @Test
  public void publish_ShouldReportErrorWhenStreamThrowsException() {
    final Exception exception = new RuntimeException();
    doThrow(exception).when(out).print(anyString());

    consoleHandler.publish(newLogRecordForLevel(Level.INFO));

    verify(errorManager).error(any(), eq(exception), eq(ErrorManager.WRITE_FAILURE));
  }
}
