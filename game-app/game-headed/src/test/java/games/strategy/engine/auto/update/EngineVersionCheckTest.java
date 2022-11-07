package games.strategy.engine.auto.update;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import games.strategy.triplea.settings.ClientSetting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.sonatype.goodies.prefs.memory.MemoryPreferences;

final class EngineVersionCheckTest {

  @BeforeEach
  void setUp() {
    ClientSetting.setPreferences(new MemoryPreferences());
  }

  @AfterEach
  void tearDown() {
    ClientSetting.resetPreferences();
  }

  @DisplayName("If there is no last updated engine check set, then we need to check")
  @Test
  void engineUpdateCheckRequired_lastCheckNeverRun() {
    ClientSetting.lastCheckForEngineUpdate.setValueAndFlush(0L);

    final boolean checkNeeded = EngineVersionCheck.isEngineUpdateCheckRequired();

    assertThat(checkNeeded, is(true));
  }

  @DisplayName("If last updated engine check is earlier than the cut-off, then we need to check")
  @Test
  void engineUpdateCheckRequired_beforeCutoff() {
    ClientSetting.lastCheckForEngineUpdate.setValueAndFlush(
        Instant.now()
            .minus((EngineVersionCheck.CHECK_FREQUENCY_IN_DAYS + 1), ChronoUnit.DAYS)
            .toEpochMilli());

    final boolean checkNeeded = EngineVersionCheck.isEngineUpdateCheckRequired();

    assertThat(checkNeeded, is(true));
  }

  @DisplayName("If last updated engine check is after the cut-off, then we do not need to check")
  @Test
  void engineUpdateCheckRequired_afterCutoff() {

    ClientSetting.lastCheckForEngineUpdate.setValueAndFlush(
        Instant.now()
            .minus((EngineVersionCheck.CHECK_FREQUENCY_IN_DAYS - 1), ChronoUnit.DAYS)
            .toEpochMilli());

    final boolean checkNeeded = EngineVersionCheck.isEngineUpdateCheckRequired();

    assertThat(checkNeeded, is(false));
  }
}
