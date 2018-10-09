package games.strategy.debug.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.text.IsEmptyString.isEmptyString;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

/**
 * In this test we will exercise the various ways to build an Error report object.
 */
class UserErrorReportTest {

  private static final String DESCRIPTION = "Lanista, absolutio, et demissio.";

  @Test
  void withLogRecord() {
    assertThat(
        UserErrorReport.builder()
            .logRecord(new LogRecord(Level.FINER, DESCRIPTION))
            .build()
            .toErrorReport()
            .getErrorMessageToUser(),
        is(DESCRIPTION));
  }

  @Test
  void gameVersionIsSet() {
    assertThat(
        UserErrorReport.builder()
            .build()
            .toErrorReport()
            .getGameVersion(),
        not(isEmptyString()));
  }
}
