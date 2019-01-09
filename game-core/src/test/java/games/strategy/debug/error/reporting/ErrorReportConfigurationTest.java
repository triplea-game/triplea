package games.strategy.debug.error.reporting;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;


@Integration
class ErrorReportConfigurationTest extends AbstractClientSettingTestCase {

  @Test
  void verifyConstructionIsWithoutError() {
    ErrorReportConfiguration.newReportHandler();
    ErrorReportConfiguration.newReportHandler(Optional::empty);
    ErrorReportConfiguration.newReportHandler(() -> Optional.of("http://sample.uri"));
  }
}
