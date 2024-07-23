package games.strategy.engine.framework;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.GameDataFileUtils.addExtension;

import games.strategy.engine.data.GameStep;
import java.nio.file.Path;
import java.util.Optional;

/** Headless variant of {@link AutoSaveFileUtils} with slightly shortened save-game names. */
public class HeadlessAutoSaveFileUtils extends AutoSaveFileUtils {
  @Override
  String getAutoSaveFileName(final String baseFileName) {
    return "autosave_"
        + Optional.ofNullable(System.getProperty(TRIPLEA_NAME)).map(v -> v + "_").orElse("")
        + baseFileName;
  }

  @Override
  public String getAutoSaveStepName(final String stepName) {
    return GameStep.isNonCombatMoveStep(stepName) ? "NonCombatMove" : "CombatMove";
  }

  public Path getHeadlessAutoSaveFile() {
    return getAutoSaveFile(addExtension("autosave"));
  }
}
