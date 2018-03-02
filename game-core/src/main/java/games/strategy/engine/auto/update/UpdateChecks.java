package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.ArgParser.CliProperties.TRIPLEA_GAME;

import java.util.Collection;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.auto.health.check.LocalSystemChecker;
import games.strategy.engine.framework.map.download.MapDownloadController;

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
    if (!UpdateCheckDecision.shouldRun()) {
      return;
    }

    // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (fileName.trim().length() > 0) {
      return;
    }

    TutorialMapDecision.checkForTutorialMap();
    EngineVersionCheck.checkForLatestEngineVersionOut();

    if (UpdateCheckDecision.shouldRunMapUpdateCheck()) {
      MapDownloadController.checkDownloadedMapsAreLatest();
    }
  }
}
