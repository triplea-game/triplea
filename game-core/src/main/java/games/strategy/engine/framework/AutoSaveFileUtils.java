package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.GameDataFileUtils.addExtension;
import static games.strategy.util.StringUtils.capitalize;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.triplea.settings.ClientSetting;

/**
 * Provides methods for getting the names of auto-save files periodically generated during a game.
 */
public final class AutoSaveFileUtils {
  private AutoSaveFileUtils() {}

  @VisibleForTesting
  static File getAutoSaveFile(final String fileName) {
    return ClientSetting.saveGamesFolderPath.getValueOrThrow().resolve(Paths.get("autoSave", fileName)).toFile();
  }

  private static File getAutoSaveFile(final String baseFileName, final boolean headless) {
    return getAutoSaveFile(getAutoSaveFileName(baseFileName, headless));
  }

  @VisibleForTesting
  static String getAutoSaveFileName(final String baseFileName, final boolean headless) {
    if (headless) {
      final String prefix = System.getProperty(TRIPLEA_NAME, "");
      if (!prefix.isEmpty()) {
        return prefix + "_" + baseFileName;
      }
    }
    return baseFileName;
  }

  public static File getHeadlessAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave"), true);
  }

  public static File getOddRoundAutoSaveFile(final boolean headless) {
    return getAutoSaveFile(addExtension("autosave_round_odd"), headless);
  }

  public static File getEvenRoundAutoSaveFile(final boolean headless) {
    return getAutoSaveFile(addExtension("autosave_round_even"), headless);
  }

  public static File getLostConnectionAutoSaveFile(final LocalDateTime localDateTime) {
    checkNotNull(localDateTime);

    return getAutoSaveFile(
        addExtension("connection_lost_on_" + DateTimeFormatter.ofPattern("MMM_dd_'at'_HH_mm").format(localDateTime)));
  }

  public static File getBeforeStepAutoSaveFile(final String stepName, final boolean headless) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveBefore" + capitalize(stepName)), headless);
  }

  public static File getAfterStepAutoSaveFile(final String stepName, final boolean headless) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveAfter" + capitalize(stepName)), headless);
  }
}
