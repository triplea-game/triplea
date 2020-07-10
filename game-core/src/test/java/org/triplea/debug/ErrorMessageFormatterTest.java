package org.triplea.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.when;

import games.strategy.triplea.UrlConstants;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErrorMessageFormatterTest {

  private static final String LOG_MESSAGE = "Pants travel with power at the stormy madagascar!";
  private static final String EXCEPTION_MESSAGE = "Jolly, yer not trading me without a life!";

  private static final Exception EXCEPTION_WITHOUT_MESSAGE = new NullPointerException();
  private static final Exception EXCEPTION_WITH_MESSAGE =
      new NullPointerException(EXCEPTION_MESSAGE);

  @Mock private LogRecord logRecord;

  private final Function<LogRecord, String> errorMessageFormatter = new ErrorMessageFormatter();

  @Test
  void logMessageOnly() {
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getLevel()).thenReturn(Level.SEVERE);
    when(logRecord.getThrown()).thenReturn(null);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(result, is("<html>" + LOG_MESSAGE + "</html>"));
  }

  @Test
  void exceptionOnlyWithMessage() {
    givenLogRecordWithNoMessageOnlyAnException(logRecord, EXCEPTION_WITH_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + EXCEPTION_WITH_MESSAGE.getClass().getSimpleName()
                + " - "
                + EXCEPTION_WITH_MESSAGE.getMessage()
                + "</html>"));
  }

  private static void givenLogRecordWithNoMessageOnlyAnException(
      final LogRecord logRecord, final Exception exception) {
    when(logRecord.getMessage()).thenReturn(exception.getMessage());
    when(logRecord.getLevel()).thenReturn(Level.SEVERE);
    when(logRecord.getThrown()).thenReturn(exception);
  }

  @Test
  void exceptionOnlyWithNoMessage() {
    givenLogRecordWithNoMessageOnlyAnException(logRecord, EXCEPTION_WITHOUT_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result, is("<html>" + EXCEPTION_WITHOUT_MESSAGE.getClass().getSimpleName() + "</html>"));
  }

  @Test
  void bothMessageAndExceptionWithExceptionMessage() {
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getLevel()).thenReturn(Level.SEVERE);
    when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + LOG_MESSAGE
                + "<br/><br/>"
                + EXCEPTION_WITH_MESSAGE.getClass().getSimpleName()
                + ": "
                + EXCEPTION_WITH_MESSAGE.getMessage()
                + "</html>"));
  }

  @Test
  void messageAndExceptionWithoutExceptionMessage() {
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getLevel()).thenReturn(Level.SEVERE);
    when(logRecord.getThrown()).thenReturn(EXCEPTION_WITHOUT_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + LOG_MESSAGE
                + "<br/><br/>"
                + EXCEPTION_WITHOUT_MESSAGE.getClass().getSimpleName()
                + "</html>"));
  }

  @Test
  void warningLevelAppendsBugReportLink() {
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getLevel()).thenReturn(Level.WARNING);
    when(logRecord.getThrown()).thenReturn(EXCEPTION_WITHOUT_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(result, containsString(UrlConstants.GITHUB_ISSUES));
  }
}
