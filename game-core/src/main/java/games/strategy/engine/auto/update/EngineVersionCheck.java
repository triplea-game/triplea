package games.strategy.engine.auto.update;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.settings.GameSetting;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.util.Version;

@Log
@UtilityClass
final class EngineVersionCheck {
  private static final String TRIPLEA_VERSION_LINK =
      "https://raw.githubusercontent.com/triplea-game/triplea/master/latest_version.properties";

  static void checkForLatestEngineVersionOut() {
    try {
      if (!isEngineUpdateCheckRequired()) {
        return;
      }

      final Version latestVersionOut =
          new Version(
              getProperties().getProperty("LATEST", ClientContext.engineVersion().toString()));

      if (latestVersionOut.isGreaterThan(ClientContext.engineVersion())) {
        SwingUtilities.invokeLater(
            () ->
                EventThreadJOptionPane.showMessageDialog(
                    null,
                    OutOfDateDialog.getOutOfDateComponent(latestVersionOut),
                    "Please Update TripleA",
                    JOptionPane.INFORMATION_MESSAGE));
      }
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Error while checking for engine updates", e);
    }
  }

  private static Properties getProperties() {
    final Properties props = new Properties();
    try {
      props.load(new URL(TRIPLEA_VERSION_LINK).openStream());
    } catch (final IOException e) {
      log.info("Failed to get TripleA latest version file, check internet connection, error: " + e);
    }
    return props;
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
