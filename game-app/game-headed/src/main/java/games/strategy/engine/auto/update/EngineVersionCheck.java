package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.experimental.UtilityClass;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.live.servers.LiveServersFetcher;
import org.triplea.util.Version;

@UtilityClass
final class EngineVersionCheck {

  public static final int CHECK_FREQUENCY_IN_DAYS = 2;

  static void checkForLatestEngineVersionOut() {
    if (!isEngineUpdateCheckRequired()) {
      return;
    }
    ClientSetting.lastCheckForEngineUpdate.setValueAndFlush(Instant.now().toEpochMilli());

    LiveServersFetcher.latestVersion()
        .filter(
            latestVersion ->
                new Version(latestVersion.getLatestEngineVersion())
                    .isGreaterThan(ProductVersionReader.getCurrentVersion()))
        .ifPresent(OutOfDateDialog::showOutOfDateComponent);
  }

  @VisibleForTesting
  static boolean isEngineUpdateCheckRequired() {
    return Instant.ofEpochMilli(ClientSetting.lastCheckForEngineUpdate.getValue().orElseThrow())
        .isBefore(Instant.now().minus(CHECK_FREQUENCY_IN_DAYS, ChronoUnit.DAYS));
  }
}
