package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.triplea.injection.Injections;
import org.triplea.live.servers.LiveServersFetcher;
import org.triplea.swing.EventThreadJOptionPane;

@Log
@UtilityClass
final class EngineVersionCheck {

  static void checkForLatestEngineVersionOut() {
    if (!isEngineUpdateCheckRequired()) {
      return;
    }

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

  private static boolean isEngineUpdateCheckRequired() {
    return isEngineUpdateCheckRequired(
        LocalDate.now(ZoneId.systemDefault()),
        ClientSetting.firstTimeThisVersion,
        ClientSetting.lastCheckForEngineUpdate,
        ClientSetting::flush);
  }

  @VisibleForTesting
  static boolean isEngineUpdateCheckRequired(
      final LocalDate now,
      final GameSetting<Boolean> firstRunSetting,
      final GameSetting<String> updateCheckDateSetting,
      final Runnable flushSetting) {
    // check at most once per 2 days (but still allow a 'first run message' for a new version of
    // TripleA)
    final boolean updateCheckRequired =
        firstRunSetting.getValueOrThrow()
            || updateCheckDateSetting
                .getValue()
                .map(
                    encodedUpdateCheckDate ->
                        !parseUpdateCheckDate(encodedUpdateCheckDate).isAfter(now.minusDays(2)))
                .orElse(true);
    if (!updateCheckRequired) {
      return false;
    }

    updateCheckDateSetting.setValue(formatUpdateCheckDate(now));
    flushSetting.run();
    return true;
  }

  @VisibleForTesting
  static LocalDate parseUpdateCheckDate(final String encodedUpdateCheckDate) {
    final List<String> tokens = Splitter.on(':').splitToList(encodedUpdateCheckDate);
    return LocalDate.ofYearDay(Integer.parseInt(tokens.get(0)), Integer.parseInt(tokens.get(1)));
  }

  @VisibleForTesting
  static String formatUpdateCheckDate(final LocalDate updateCheckDate) {
    return updateCheckDate.getYear() + ":" + updateCheckDate.getDayOfYear();
  }
}
