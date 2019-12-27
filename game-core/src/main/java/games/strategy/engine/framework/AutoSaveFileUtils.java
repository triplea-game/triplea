package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.GameDataFileUtils.addExtension;
import static org.triplea.java.StringUtils.capitalize;

import games.strategy.engine.framework.ui.SaveGameFileChooser;
import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Provides methods for getting the names of auto-save files periodically generated during a game.
 */
public class AutoSaveFileUtils {
  File getAutoSaveFile(final String baseFileName) {
    return SaveGameFileChooser.getSaveGameFolder()
        .toPath()
        .resolve(Paths.get("autoSave", getAutoSaveFileName(baseFileName)))
        .toFile();
  }

  String getAutoSaveFileName(final String baseFileName) {
    return baseFileName;
  }

  public File getOddRoundAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave_round_odd"));
  }

  public File getEvenRoundAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave_round_even"));
  }

  public File getLostConnectionAutoSaveFile(final LocalDateTime localDateTime) {
    checkNotNull(localDateTime);

    return getAutoSaveFile(
        addExtension(
            "connection_lost_on_"
                + DateTimeFormatter.ofPattern("MMM_dd_'at'_HH_mm", Locale.ENGLISH)
                    .format(localDateTime)));
  }

  public File getBeforeStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveBefore" + capitalize(stepName)));
  }

  public File getAfterStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveAfter" + capitalize(stepName)));
  }

  public String getAutoSaveStepName(final String stepName) {
    return stepName;
  }
}
