package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;

@UtilityClass
class UpdatedMapsCheck {

  static final int THRESHOLD_DAYS = 7;

  static boolean isMapUpdateCheckRequired() {
    return isMapUpdateCheckRequired(
        ClientSetting.lastCheckForMapUpdates.getValue().orElse(0L),
        () -> ClientSetting.lastCheckForMapUpdates.setValueAndFlush(Instant.now().toEpochMilli()));
  }

  @VisibleForTesting
  static boolean isMapUpdateCheckRequired(
      final long lastCheckEpochMilli, final Runnable lastCheckSetter) {
    final Instant cutOff = Instant.now().minus(THRESHOLD_DAYS, ChronoUnit.DAYS);
    final Instant lastCheck = Instant.ofEpochMilli(lastCheckEpochMilli);

    lastCheckSetter.run();

    return lastCheck.isBefore(cutOff);
  }
}
