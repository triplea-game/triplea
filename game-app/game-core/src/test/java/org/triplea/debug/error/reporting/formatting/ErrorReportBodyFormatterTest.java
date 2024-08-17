package org.triplea.debug.error.reporting.formatting;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.system.SystemProperties;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.debug.ExceptionDetails;
import org.triplea.debug.LoggerRecord;
import org.triplea.util.Version;

@ExtendWith(MockitoExtension.class)
class ErrorReportBodyFormatterTest {
  @NonNls private static final String SAMPLE_USER_DESCRIPTION = "Pol, a bene vortex";
  private static final String LOG_MESSAGE =
      "LogMessage, Golly gosh, yer not drinking me without a desolation!";
  private static final Exception EXCEPTION_WITH_MESSAGE =
      new RuntimeException("simulated exception");
  private static final Exception EXCEPTION_WITH_CAUSE =
      new RuntimeException(EXCEPTION_WITH_MESSAGE);

  @Mock private LoggerRecord logRecord;

  @Test
  void containsUseSuppliedData() {
    final String body =
        ErrorReportBodyFormatter.buildBody(
            SAMPLE_USER_DESCRIPTION, "mapName", logRecord, new Version("2.0.0"));

    assertThat(body, containsString(SAMPLE_USER_DESCRIPTION));
  }

  @Test
  void containsMapName() {
    final String body =
        ErrorReportBodyFormatter.buildBody(
            SAMPLE_USER_DESCRIPTION, "mapName", logRecord, new Version("2.0.0"));

    assertThat(body, containsString("mapName"));
  }

  @Test
  void containsSystemData() {
    final String body =
        ErrorReportBodyFormatter.buildBody(
            SAMPLE_USER_DESCRIPTION, "mapName", logRecord, new Version("2.0.0"));

    assertThat(body, containsString(SAMPLE_USER_DESCRIPTION));
    assertThat(body, containsString(SystemProperties.getOperatingSystem()));
    assertThat(body, containsString(SystemProperties.getJavaVersion()));
    assertThat(body, containsString(new Version("2.0.0").toString()));
  }

  @Test
  void containsStackTraceData() {
    when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .stackTraceElements(EXCEPTION_WITH_CAUSE.getStackTrace())
                    .exceptionMessage(EXCEPTION_WITH_CAUSE.getMessage())
                    .exceptionClassName(EXCEPTION_WITH_CAUSE.getClass().getSimpleName())
                    .build(),
                ExceptionDetails.builder()
                    .stackTraceElements(EXCEPTION_WITH_MESSAGE.getStackTrace())
                    .exceptionMessage(EXCEPTION_WITH_MESSAGE.getMessage())
                    .exceptionClassName(EXCEPTION_WITH_MESSAGE.getClass().getSimpleName())
                    .build()));

    final String body =
        ErrorReportBodyFormatter.buildBody(
            SAMPLE_USER_DESCRIPTION, "mapName", logRecord, new Version("2.0.0"));

    Stream.of(EXCEPTION_WITH_CAUSE, EXCEPTION_WITH_MESSAGE)
        .map(Throwable::getStackTrace)
        .flatMap(Arrays::stream)
        .forEach(
            trace ->
                assertThat(
                    "should contain each element of stack trace",
                    body,
                    containsString(trace.toString())));

    assertThat(
        "should contain message of cause",
        body,
        containsString(EXCEPTION_WITH_MESSAGE.getMessage()));
  }

  @Test
  void containsExceptionMessageAndLogMessageWhenBothArePresent() {
    when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getExceptions())
        .thenReturn(
            List.of(
                ExceptionDetails.builder()
                    .exceptionClassName(EXCEPTION_WITH_MESSAGE.getClass().getName())
                    .exceptionMessage(EXCEPTION_WITH_MESSAGE.getMessage())
                    .build()));

    final String body =
        ErrorReportBodyFormatter.buildBody(
            SAMPLE_USER_DESCRIPTION, "mapName", logRecord, new Version("2.0.0"));

    assertThat(body, containsString(EXCEPTION_WITH_MESSAGE.getClass().getName()));
    assertThat(body, containsString(LOG_MESSAGE));
  }

  @Test
  void nullLogMessage() {
    when(logRecord.getLogMessage()).thenReturn(LOG_MESSAGE);
    when(logRecord.getExceptions())
        .thenReturn(
            List.of(ExceptionDetails.builder().exceptionClassName("NullPointerException").build()));

    final String body =
        ErrorReportBodyFormatter.buildBody(
            SAMPLE_USER_DESCRIPTION, "mapName", logRecord, new Version("2.0.0"));

    assertThat(body, containsString("NullPointerException"));
  }
}
