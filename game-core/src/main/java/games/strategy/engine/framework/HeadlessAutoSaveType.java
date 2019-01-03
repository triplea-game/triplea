package games.strategy.engine.framework;

import java.io.File;
import java.nio.file.Path;

/** The types of auto-saves that can be loaded by a headless game server. */
public enum HeadlessAutoSaveType {
  DEFAULT(AutoSaveFileUtils.getHeadlessAutoSaveFile()),

  ODD_ROUND(AutoSaveFileUtils.getOddRoundAutoSaveFile(true)),

  EVEN_ROUND(AutoSaveFileUtils.getEvenRoundAutoSaveFile(true)),

  END_TURN(AutoSaveFileUtils.getBeforeStepAutoSaveFile("EndTurn", true)),

  BEFORE_BATTLE(AutoSaveFileUtils.getBeforeStepAutoSaveFile("Battle", true)),

  AFTER_BATTLE(AutoSaveFileUtils.getAfterStepAutoSaveFile("Battle", true)),

  AFTER_COMBAT_MOVE(AutoSaveFileUtils.getAfterStepAutoSaveFile("CombatMove", true)),

  AFTER_NON_COMBAT_MOVE(AutoSaveFileUtils.getAfterStepAutoSaveFile("NonCombatMove", true));

  private final Path path;

  HeadlessAutoSaveType(final File file) {
    path = file.toPath();
  }

  public File getFile() {
    return path.toFile();
  }
}
