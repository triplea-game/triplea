package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import java.awt.Component;
import org.triplea.java.ThreadRunner;

/**
 * Runs background update checks and would prompt user if anything needs to be updated. This class
 * and related ones will control the frequency of how often we prompt the user.
 */
public final class UpdateChecks {
  private UpdateChecks() {}

  public static void launch(final Component parentComponent) {
    ThreadRunner.runInNewThread(() -> UpdateChecks.checkForUpdates(parentComponent));
  }

  private static void checkForUpdates(final Component parentComponent) {
    if (!shouldRun()) {
      return;
    }

    TutorialMapCheck.checkForTutorialMap();
    EngineVersionCheck.checkForLatestEngineVersionOut(parentComponent);
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
