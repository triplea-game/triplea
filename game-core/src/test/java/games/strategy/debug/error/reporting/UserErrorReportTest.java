package games.strategy.debug.error.reporting;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;

/**
 * In this test we will exercise the various ways to build an Error report object.
 */
class UserErrorReportTest {

  @Test
  void javaVersionIsSet() {
    assertThat(
        UserErrorReport.builder()
            .build()
            .toErrorReport()
            .getJavaVersion(),
        is(not(emptyString())));
  }

  @Test
  void gameVersionIsSet() {
    assertThat(
        UserErrorReport.builder()
            .build()
            .toErrorReport()
            .getGameVersion(),
        is(not(emptyString())));
  }
}
