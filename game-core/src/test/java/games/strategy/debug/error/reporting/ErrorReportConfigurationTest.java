package games.strategy.debug.error.reporting;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;
import org.triplea.test.common.Integration;

import games.strategy.triplea.settings.ClientSetting;


@Integration
class ErrorReportConfigurationTest {

  @Test
  void verifyConstructionIsWithoutError() {
    ClientSetting.setPreferences(new MemoryPreferences());
    ErrorReportConfiguration.newReportHandler();
    ErrorReportConfiguration.newReportHandler(Optional::empty);
    ErrorReportConfiguration.newReportHandler(() -> Optional.of("http://sample.uri"));
  }
}
