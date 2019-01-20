package games.strategy.engine.auto.update;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.util.EventThreadJOptionPane;
import lombok.extern.java.Log;

@Log
final class EngineVersionCheck {
  private EngineVersionCheck() {}

  static void checkForLatestEngineVersionOut() {
    try {
      if (!isEngineUpdateCheckRequired()) {
        return;
      }

      final EngineVersionProperties latestEngineOut = new EngineVersionProperties();

      if (ClientContext.engineVersion().isLessThan(latestEngineOut.getLatestVersionOut())) {
        SwingUtilities
            .invokeLater(() -> EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(),
                "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE));
      }
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error while checking for engine updates", e);
    }
  }

  private static boolean isEngineUpdateCheckRequired() {
    return isEngineUpdateCheckRequired(
        LocalDate.now(),
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
    // check at most once per 2 days (but still allow a 'first run message' for a new version of TripleA)
    final boolean updateCheckRequired = firstRunSetting.getValueOrThrow()
        || updateCheckDateSetting.getValue()
            .map(encodedUpdateCheckDate -> !parseUpdateCheckDate(encodedUpdateCheckDate).isAfter(now.minusDays(2)))
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
