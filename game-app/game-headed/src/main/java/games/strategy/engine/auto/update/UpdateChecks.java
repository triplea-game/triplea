package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import org.triplea.java.ThreadRunner;

/**
 * Runs background update checks and would prompt user if anything needs to be updated. This class
 * and related ones will control the frequency of how often we prompt the user.
 */
public final class UpdateChecks {
  private UpdateChecks() {}

  public static void launch() {
    ThreadRunner.runInNewThread(UpdateChecks::checkForUpdates);
  }

  private static void checkForUpdates() {
    if (!shouldRun()) {
      return;
    }

    TutorialMapCheck.checkForTutorialMap();
    EngineVersionCheck.checkForLatestEngineVersionOut();
    UpdatedMapsCheck.checkDownloadedMapsAreLatest();
  }

  private static boolean shouldRun() {
    // if we are joining a game online, or hosting, or loading straight into a save game, do not
    // check
    return !System.getProperty(TRIPLEA_SERVER, "false").equalsIgnoreCase("true")
        && !System.getProperty(TRIPLEA_CLIENT, "false").equalsIgnoreCase("true")
        && System.getProperty(TRIPLEA_GAME, "").isEmpty();
  }
}
