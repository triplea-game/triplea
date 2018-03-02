package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.ArgParser.CliProperties.DO_NOT_CHECK_FOR_UPDATES;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_SERVER;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Collection;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.EngineVersionProperties;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.systemcheck.LocalSystemChecker;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.SwingComponents;
import games.strategy.util.EventThreadJOptionPane;

/**
 * Runs background update checks and would prompt user if anything needs to be updated.
 * This class and related ones will control the frequency of how often we prompt the user.
 */
public class UpdateChecks {

  public static void launch() {
    new Thread(UpdateChecks::checkLocalSystem).start();
    new Thread(UpdateChecks::checkForUpdates).start();
  }

  private static void checkLocalSystem() {
    final LocalSystemChecker localSystemChecker = new LocalSystemChecker();
    final Collection<Exception> exceptions = localSystemChecker.getExceptions();
    if (!exceptions.isEmpty()) {
      final String msg = String.format(
          "Warning!! %d system checks failed. Some game features may not be available or may not work correctly.\n%s",
          exceptions.size(), localSystemChecker.getStatusMessage());
      ClientLogger.logError(msg);
    }
  }

  private static void checkForUpdates() {
    new Thread(() -> {
      if (System.getProperty(TRIPLEA_SERVER, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(TRIPLEA_CLIENT, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true")) {
        return;
      }

      // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
      final String fileName = System.getProperty(TRIPLEA_GAME, "");
      if (fileName.trim().length() > 0) {
        return;
      }

      boolean busy = checkForTutorialMap();
      if (!busy) {
        busy = checkForLatestEngineVersionOut();
      }
      if (!busy) {
        checkForUpdatedMaps();
      }
    }, "Checking Latest TripleA Engine Version").start();
  }

  /**
   * Returns true if we are out of date or this is the first time this triplea has ever been run.
   */
  private static boolean checkForLatestEngineVersionOut() {
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
          return false;
        }
      }

      ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.save(year + ":" + day);
      ClientSetting.flush();

      final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
      if (latestEngineOut == null) {
        return false;
      }
      if (ClientContext.engineVersion().isLessThan(latestEngineOut.getLatestVersionOut())) {
        SwingUtilities
            .invokeLater(() -> EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(),
                "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE));
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for engine updates", e);
    }
    return false;
  }

  private static boolean checkForTutorialMap() {
    final MapDownloadController mapDownloadController = ClientContext.mapDownloadController();
    final boolean promptToDownloadTutorialMap = mapDownloadController.shouldPromptToDownloadTutorialMap();
    mapDownloadController.preventPromptToDownloadTutorialMap();
    if (!promptToDownloadTutorialMap) {
      return false;
    }

    final String message = "<html>Would you like to download the tutorial map?<br><br>"
        + "(You can always download it later using the Download Maps<br>"
        + "command if you don't want to do it now.)</html>";
    SwingComponents.promptUser("Welcome to TripleA", message, () -> {
      DownloadMapsWindow.showDownloadMapsWindowAndDownload("Tutorial");
    });
    return true;
  }

  private static void checkForUpdatedMaps() {
    final MapDownloadController downloadController = ClientContext.mapDownloadController();
    downloadController.checkDownloadedMapsAreLatest();
  }

}
