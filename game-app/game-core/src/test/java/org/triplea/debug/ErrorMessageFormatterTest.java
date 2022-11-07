package org.triplea.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.lenient;

import games.strategy.triplea.UrlConstants;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErrorMessageFormatterTest {

  private static final String LOG_MESSAGE = "Pants travel with power at the stormy madagascar!";

  @Mock private LoggerRecord logRecord;

  private final Function<LoggerRecord, String> errorMessageFormatter = new ErrorMessageFormatter();

  @Test
  void logMessageOnly() {
    lenient().when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient().when(logRecord.getExceptions()).thenReturn(List.of());

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(result, is("<html><b>" + LOG_MESSAGE + "</b></html>"));
  }

  @Test
  void exceptionOnlyWithNoMessage() {
    lenient().when(logRecord.getLogMessage()).thenReturn(null);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .exceptionClassName("java.lang.NullPointerException")
                    .exceptionMessage(null)
                    .build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html><b>"
                + ErrorMessageFormatter.UNEXPECTED_ERROR_TEXT
                + "</b><br/><br/>NullPointerException</html>"));
  }

  @Test
  void bothMessageAndExceptionWithExceptionMessage() {
    lenient().when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .exceptionMessage("message from exception222")
                    .exceptionClassName("NullPointerException222")
                    .build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html><b>"
                + LOG_MESSAGE
                + "</b><br/><br/>NullPointerException222: message from exception222</html>"));
  }

  @Test
  void duplicatLogMessageIsHandledAsUnknownError() {
    lenient().when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .exceptionClassName("java.lang.exceptionClass")
                    .exceptionMessage(LOG_MESSAGE)
                    .build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        "exception has a duplicate message as the log statement, this indicates the"
            + "log message was derived from the exception and we are looking "
            + "at an uncaught exception.",
        result,
        is(
            "<html><b>"
                + ErrorMessageFormatter.UNEXPECTED_ERROR_TEXT
                + "</b><br/><br/>exceptionClass: "
                + LOG_MESSAGE
                + "</html>"));
  }

  @Test
  void messageAndExceptionWithoutExceptionMessage() {
    lenient().when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder().exceptionClassName("NullPointerException4444").build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result, is("<html><b>" + LOG_MESSAGE + "</b><br/><br/>NullPointerException4444</html>"));
  }

  @Test
  void warningLevelAppendsBugReportLink() {
    lenient().when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    lenient().when(logRecord.isError()).thenReturn(false);
    lenient().when(logRecord.isWarning()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder().exceptionClassName("NullPointerException6").build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(result, containsString(UrlConstants.GITHUB_ISSUES));
  }
}
