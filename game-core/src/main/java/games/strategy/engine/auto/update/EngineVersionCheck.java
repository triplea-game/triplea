package games.strategy.engine.auto.update;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

      final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
      if (latestEngineOut == null) {
        return;
      }
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
        ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY,
        ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE,
        ClientSetting::flush);
  }

  @VisibleForTesting
  static boolean isEngineUpdateCheckRequired(
      final LocalDate now,
      final GameSetting firstRunSetting,
      final GameSetting updateCheckDateSetting,
      final Runnable flushSetting) {
    // check at most once per 2 days (but still allow a 'first run message' for a new version of TripleA)
    final boolean firstRun = firstRunSetting.booleanValue();
    final String encodedUpdateCheckDate = updateCheckDateSetting.value();
    if (!firstRun && !encodedUpdateCheckDate.trim().isEmpty()) {
      final LocalDate updateCheckDate = parseUpdateCheckDate(encodedUpdateCheckDate);
      if (updateCheckDate.until(now, ChronoUnit.DAYS) < 2) {
        return false;
      }
    }

    updateCheckDateSetting.save(formatUpdateCheckDate(now));
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
