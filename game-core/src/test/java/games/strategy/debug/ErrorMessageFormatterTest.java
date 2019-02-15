package games.strategy.debug;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;

import java.util.function.Function;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ErrorMessageFormatterTest {

  private static final String CLASS_NAME = "UserErrorReportSample";
  private static final String METHOD_NAME = "formatSomething";
  private static final String LOCATION_LINE = "Location: " + CLASS_NAME + "." + METHOD_NAME;

  private static final String LOG_MESSAGE = "Pants travel with power at the stormy madagascar!";
  private static final String EXCEPTION_MESSAGE = "Jolly, yer not trading me without a life!";


  private static final Exception EXCEPTION_WITHOUT_MESSAGE = new NullPointerException();
  private static final Exception EXCEPTION_WITH_MESSAGE = new NullPointerException(EXCEPTION_MESSAGE);

  @Mock
  private LogRecord logRecord;

  private Function<LogRecord, String> errorMessageFormatter;

  @BeforeEach
  void setup() {
    errorMessageFormatter = new ErrorMessageFormatter();
  }


  /*
   * <pre>
   * Error: {log message}
   *
   * Location: {className}.{methodName}
   * </pre>
   */
  @Test
  void logMessageOnly() {
    givenSourceClassAndMethodName(logRecord);
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getThrown()).thenReturn(null);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + "Error: " + LOG_MESSAGE + "<br/><br/>"
                + LOCATION_LINE
                + "</html>"));
  }

  private static void givenSourceClassAndMethodName(final LogRecord logRecord) {
    when(logRecord.getSourceClassName()).thenReturn(CLASS_NAME);
    when(logRecord.getSourceMethodName()).thenReturn(METHOD_NAME);
  }

  /*
   * <pre>
   * Error: {exception simple name} - {exception message}
   *
   * Location: {className}.{methodName}
   * </pre>
   */
  @Test
  void exceptionOnlyWithMessage() {
    givenSourceClassAndMethodName(logRecord);
    givenLogRecordWithNoMessageOnlyAnException(logRecord, EXCEPTION_WITH_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + "Error: " + EXCEPTION_WITH_MESSAGE.getClass().getSimpleName() + " - "
                + EXCEPTION_WITH_MESSAGE.getMessage() + "<br/><br/>"
                + LOCATION_LINE
                + "</html>"));
  }

  private static void givenLogRecordWithNoMessageOnlyAnException(final LogRecord logRecord, final Exception exception) {
    when(logRecord.getMessage()).thenReturn(exception.getMessage());
    when(logRecord.getThrown()).thenReturn(exception);
  }

  /*
   * <pre>
   * Error: {exception simple name}
   *
   * Location: {className}.{methodName}
   * </pre>
   */
  @Test
  void exceptionOnlyWithNoMessage() {
    givenSourceClassAndMethodName(logRecord);
    givenLogRecordWithNoMessageOnlyAnException(logRecord, EXCEPTION_WITHOUT_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + "Error: " + EXCEPTION_WITHOUT_MESSAGE.getClass().getSimpleName() + "<br/><br/>"
                + LOCATION_LINE
                + "</html>"));
  }

  /*
   * <pre>
   * Error: {log message}
   *
   * Details: {exception simple name} - {exception message}
   *
   * Location: {className}.{methodName}
   * </pre>
   */
  @Test
  void bothMessageAndExceptionWithExceptionMessage() {
    givenSourceClassAndMethodName(logRecord);
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getThrown()).thenReturn(EXCEPTION_WITH_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + "Error: " + LOG_MESSAGE + "<br/><br/>"
                + "Details: " + EXCEPTION_WITH_MESSAGE.getClass().getSimpleName() + " - "
                + EXCEPTION_WITH_MESSAGE.getMessage() + "<br/><br/>"
                + LOCATION_LINE
                + "</html>"));
  }

  /*
   * <pre>
   * Error: {log message}
   *
   * Details: {exception simple name}
   *
   * Location: {className}.{methodName}
   * </pre>
   */
  @Test
  void messageAndExceptionWithoutExceptionMessage() {
    givenSourceClassAndMethodName(logRecord);
    when(logRecord.getMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getThrown()).thenReturn(EXCEPTION_WITHOUT_MESSAGE);

    final String result = errorMessageFormatter.apply(logRecord);

    assertThat(
        result,
        is(
            "<html>"
                + "Error: " + LOG_MESSAGE + "<br/><br/>"
                + "Details: " + EXCEPTION_WITHOUT_MESSAGE.getClass().getSimpleName() + "<br/><br/>"
                + LOCATION_LINE
                + "</html>"));
  }
}
