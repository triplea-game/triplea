package org.triplea.debug.error.reporting.formatting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.debug.ExceptionDetails;
import org.triplea.debug.LoggerRecord;

@ExtendWith(MockitoExtension.class)
class ErrorReportTitleFormatterTest {
  private static final String LOG_MESSAGE =
      "LogMessage, Golly gosh, yer not drinking me without a desolation!";
  private static final Exception EXCEPTION_WITH_MESSAGE =
      new RuntimeException("simulated exception");
  private static final Exception EXCEPTION_WITH_NO_MESSAGE = new NullPointerException();
  private static final Exception EXCEPTION_WITH_CAUSE =
      new RuntimeException(EXCEPTION_WITH_MESSAGE);

  @Mock private LoggerRecord logRecord;

  @Test
  void logMessageOnly() {
    when(logRecord.getLoggerClassName()).thenReturn("org.ClassName");
    when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);

    final String title = ErrorReportTitleFormatter.createTitle(logRecord);

    assertThat(title, is("ClassName - " + LOG_MESSAGE));
  }

  @Test
  void logMessageOnlyWithTripleaPackage() {
    when(logRecord.getLoggerClassName()).thenReturn("org.triplea.ClassName");
    when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);

    final String title = ErrorReportTitleFormatter.createTitle(logRecord);

    assertThat(title, is("ClassName - " + LOG_MESSAGE));
  }

  @Test
  void handleNullLogMessageAndNullExceptionMessage() {
    when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .exceptionClassName(EXCEPTION_WITH_NO_MESSAGE.getClass().getSimpleName())
                    .stackTraceElements(EXCEPTION_WITH_NO_MESSAGE.getStackTrace())
                    .build()));

    final String title = ErrorReportTitleFormatter.createTitle(logRecord);

    assertThat(
        title,
        is(
            ErrorReportTitleFormatterTest.class.getSimpleName()
                + "#<clinit>:"
                + EXCEPTION_WITH_NO_MESSAGE.getStackTrace()[0].getLineNumber()
                + " - "
                + EXCEPTION_WITH_NO_MESSAGE.getClass().getSimpleName()));
  }

  @Test
  void handleClassNameWithNoPackages() {
    when(logRecord.getLoggerClassName()).thenReturn("ClassInDefaultPackage");

    assertDoesNotThrow(() -> ErrorReportTitleFormatter.createTitle(logRecord));
  }

  @Test
  void handleExceptionWithCause() {
    when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder().exceptionClassName("exception name").build(),
                ExceptionDetails.builder()
                    .stackTraceElements(EXCEPTION_WITH_CAUSE.getCause().getStackTrace())
                    .exceptionClassName(EXCEPTION_WITH_CAUSE.getClass().getSimpleName())
                    .build()));

    final String title = ErrorReportTitleFormatter.createTitle(logRecord);

    assertThat(
        title,
        is(
            ErrorReportTitleFormatterTest.class.getSimpleName()
                + "#<clinit>:"
                + EXCEPTION_WITH_CAUSE.getCause().getStackTrace()[0].getLineNumber()
                + " - "
                + EXCEPTION_WITH_CAUSE.getCause().getClass().getSimpleName()));
  }
}
