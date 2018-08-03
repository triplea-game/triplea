package games.strategy.engine.auto.update;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.EventThreadJOptionPane;
import lombok.extern.java.Log;

@Log
class EngineVersionCheck {
  static void checkForLatestEngineVersionOut() {
    try {
      final boolean firstTimeThisVersion = ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY.booleanValue();
      // check at most once per 2 days (but still allow a 'first run message' for a new version of triplea)
      final LocalDateTime localDateTime = LocalDateTime.now();
      final int year = localDateTime.get(ChronoField.YEAR);
      final int day = localDateTime.get(ChronoField.DAY_OF_YEAR);
      // format year:day
      final String lastCheckTime = ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.value();
      if (!firstTimeThisVersion && lastCheckTime.trim().length() > 0) {
        final String[] yearDay = lastCheckTime.split(":");
        if (Integer.parseInt(yearDay[0]) >= year && Integer.parseInt(yearDay[1]) + 1 >= day) {
          return;
        }
      }

      ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.save(year + ":" + day);
      ClientSetting.flush();

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
}
