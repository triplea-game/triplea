package org.triplea.http.client.error.report.create;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.hamcrest.text.IsEmptyString.emptyString;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;
import org.triplea.http.client.error.report.create.ErrorReport;
import org.triplea.http.client.error.report.create.ErrorReportDetails;

class ErrorReportTest {
  private static final String MESSAGE_TO_USER = "msg";
  private static final String EXCEPTION_MESSAGE = "exception msg";
  private static final String CLASS = "ventus";
  private static final String METHOD = "sala";

  private static final Level LEVEL = Level.SEVERE;

  private static final String GAME_VERSION = "virundum";
  private static final String TITLE = "Nunquam tractare navis.";
  private static final String DESCRIPTION = "cur humani generis crescere?";

  private static final Exception exception = new Exception(EXCEPTION_MESSAGE);

  @Test
  void errorDetailsCopiedOver() {
    final LogRecord record = new LogRecord(LEVEL, MESSAGE_TO_USER);
    record.setThrown(exception);
    record.setSourceClassName(CLASS);
    record.setSourceMethodName(METHOD);

    final ErrorReport errorReport = new ErrorReport(ErrorReportDetails.builder()
        .logRecord(record)
        .gameVersion(GAME_VERSION)
        .title(TITLE)
        .description(DESCRIPTION)
        .build());

    assertThat(errorReport.getOperatingSystem(), notNullValue());
    assertThat(errorReport.getGameVersion(), is(GAME_VERSION));
    assertThat(errorReport.getTitle(), is(TITLE));
    assertThat(errorReport.getDescription(), is(DESCRIPTION));
    assertThat(errorReport.getLogLevel(), is(LEVEL.toString()));
    assertThat(errorReport.getErrorMessageToUser(), is(MESSAGE_TO_USER));
    assertThat(errorReport.getExceptionMessage(), is(EXCEPTION_MESSAGE));
    assertThat(errorReport.getStackTrace(), containsString(ErrorReportTest.class.getName()));
    assertThat(errorReport.getClassName(), is(CLASS));
    assertThat(errorReport.getMethodName(), is(METHOD));
  }

  @Test
  void errorDetailsWhenExceptionNotPresent() {
    final LogRecord record = new LogRecord(LEVEL, EXCEPTION_MESSAGE);

    final ErrorReport errorReport = new ErrorReport(ErrorReportDetails.builder()
        .logRecord(record)
        .gameVersion(GAME_VERSION)
        .title(TITLE)
        .description(DESCRIPTION)
        .build());

    assertThat(errorReport.getExceptionMessage(), emptyString());
    assertThat(errorReport.getStackTrace(), emptyString());
  }
}
