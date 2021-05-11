package games.strategy.engine.framework;

import static com.google.common.base.Preconditions.checkNotNull;
import static games.strategy.engine.framework.GameDataFileUtils.addExtension;
import static org.triplea.java.StringUtils.capitalize;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.ClientSetting;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Provides methods for getting the names of auto-save files periodically generated during a game.
 */
public class AutoSaveFileUtils {
  @VisibleForTesting
  Path getAutoSaveFile(final String baseFileName) {
    return ClientSetting.saveGamesFolderPath
        .getValueOrThrow()
        .resolve("autoSave")
        .resolve(getAutoSaveFileName(baseFileName));
  }

  @VisibleForTesting
  String getAutoSaveFileName(final String baseFileName) {
    return baseFileName;
  }

  public Path getOddRoundAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave_round_odd"));
  }

  public Path getEvenRoundAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave_round_even"));
  }

  public Path getLostConnectionAutoSaveFile(final LocalDateTime localDateTime) {
    checkNotNull(localDateTime);

    return getAutoSaveFile(
        addExtension(
            "connection_lost_on_"
                + DateTimeFormatter.ofPattern("MMM_dd_'at'_HH_mm", Locale.ENGLISH)
                    .format(localDateTime)));
  }

  public Path getBeforeStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveBefore" + capitalize(stepName)));
  }

  public Path getAfterStepAutoSaveFile(final String stepName) {
    checkNotNull(stepName);

    return getAutoSaveFile(addExtension("autosaveAfter" + capitalize(stepName)));
  }

  public String getAutoSaveStepName(final String stepName) {
    return stepName;
  }
}
