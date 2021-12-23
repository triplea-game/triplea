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

    assertThat(result, is("<html>" + LOG_MESSAGE + "</html>"));
  }

  @Test
  void exceptionOnlyWithMessage() {
    lenient().when(logRecord.getLogMessage()).thenReturn(null);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .exceptionMessage("message from exception111")
                    .exceptionClassName("NullPointerException111")
                    .build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result, is("<html>" + "NullPointerException111" + " - message from exception111</html>"));
  }

  @Test
  void exceptionOnlyWithNoMessage() {
    lenient().when(logRecord.getLogMessage()).thenReturn(null);
    lenient().when(logRecord.isError()).thenReturn(true);
    lenient()
        .when(logRecord.getExceptions())
        .thenReturn(
            List.of(ExceptionDetails.builder().exceptionClassName("NullPointerException").build()));

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(result, is("<html>NullPointerException</html>"));
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
            "<html>"
                + LOG_MESSAGE
                + "<br/><br/>NullPointerException222: message from exception222</html>"));
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

    assertThat(result, is("<html>" + LOG_MESSAGE + "<br/><br/>NullPointerException4444</html>"));
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
