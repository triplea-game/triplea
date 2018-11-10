package games.strategy.engine.framework;

import java.io.File;
import java.nio.file.Path;

/**
 * The types of auto-saves that can be loaded by a headless game server.
 */
public enum HeadlessAutoSaveType {
  AUTOSAVE(AutoSaveFileUtils.getHeadlessAutoSaveFile()),

  AUTOSAVE_ODD(AutoSaveFileUtils.getOddRoundAutoSaveFile(true)),

  AUTOSAVE_EVEN(AutoSaveFileUtils.getEvenRoundAutoSaveFile(true)),

  AUTOSAVE_END_TURN(AutoSaveFileUtils.getBeforeStepAutoSaveFile("EndTurn", true)),

  AUTOSAVE_BEFORE_BATTLE(AutoSaveFileUtils.getBeforeStepAutoSaveFile("Battle", true)),

  AUTOSAVE_AFTER_BATTLE(AutoSaveFileUtils.getAfterStepAutoSaveFile("Battle", true)),

  AUTOSAVE_AFTER_COMBAT_MOVE(AutoSaveFileUtils.getAfterStepAutoSaveFile("CombatMove", true)),

  AUTOSAVE_AFTER_NON_COMBAT_MOVE(AutoSaveFileUtils.getAfterStepAutoSaveFile("NonCombatMove", true));

  private final Path path;

  HeadlessAutoSaveType(final File file) {
    path = file.toPath();
  }

  public File getFile() {
    return path.toFile();
  }
}
