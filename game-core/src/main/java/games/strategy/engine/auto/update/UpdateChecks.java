package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

/**
 * Runs background update checks and would prompt user if anything needs to be updated. This class
 * and related ones will control the frequency of how often we prompt the user.
 */
public final class UpdateChecks {
  private UpdateChecks() {}

  public static void launch() {
    new Thread(UpdateChecks::checkForUpdates).start();
  }

  private static void checkForUpdates() {
    if (!shouldRun()) {
      return;
    }

    TutorialMapCheck.checkForTutorialMap();
    EngineVersionCheck.checkForLatestEngineVersionOut();

    if (UpdatedMapsCheck.isMapUpdateCheckRequired()) {
      UpdatedMapsCheck.checkDownloadedMapsAreLatest();
      // TODO: add check here to prompt for tutorial map
    }
  }

  private static boolean shouldRun() {
    // if we are joining a game online, or hosting, or loading straight into a savegame, do not
    // check
    return !System.getProperty(TRIPLEA_SERVER, "false").equalsIgnoreCase("true")
        && !System.getProperty(TRIPLEA_CLIENT, "false").equalsIgnoreCase("true")
        && System.getProperty(TRIPLEA_GAME, "").isEmpty();
  }
}
