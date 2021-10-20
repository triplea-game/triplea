package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.injection.Injections;
import org.triplea.live.servers.LiveServersFetcher;
import org.triplea.swing.EventThreadJOptionPane;

@UtilityClass
final class EngineVersionCheck {

  public static final int CHECK_FREQUENCY_IN_DAYS = 2;

  static void checkForLatestEngineVersionOut() {
    if (!isEngineUpdateCheckRequired()) {
      return;
    }
    ClientSetting.lastCheckForEngineUpdate.setValueAndFlush(Instant.now().toEpochMilli());

    new LiveServersFetcher()
        .latestVersion()
        .filter(
            latestVersion ->
                latestVersion.isGreaterThan(Injections.getInstance().getEngineVersion()))
        .ifPresent(
            latestVersion ->
                SwingUtilities.invokeLater(
                    () ->
                        EventThreadJOptionPane.showMessageDialog(
                            null,
                            OutOfDateDialog.showOutOfDateComponent(latestVersion),
                            "Please Update TripleA",
                            JOptionPane.INFORMATION_MESSAGE)));
  }

  @VisibleForTesting
  static boolean isEngineUpdateCheckRequired() {
    return Instant.ofEpochMilli(ClientSetting.lastCheckForEngineUpdate.getValue().orElseThrow())
        .isBefore(Instant.now().minus(CHECK_FREQUENCY_IN_DAYS, ChronoUnit.DAYS));
  }
}
