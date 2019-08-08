package games.strategy.engine.framework;

import java.io.File;
import java.nio.file.Path;

/** The types of auto-saves that can be loaded by a headless game server. */
public enum HeadlessAutoSaveType {
  DEFAULT(new HeadlessAutoSaveFileUtils().getHeadlessAutoSaveFile()),

  ODD_ROUND(new HeadlessAutoSaveFileUtils().getOddRoundAutoSaveFile()),

  EVEN_ROUND(new HeadlessAutoSaveFileUtils().getEvenRoundAutoSaveFile()),

  END_TURN(new HeadlessAutoSaveFileUtils().getBeforeStepAutoSaveFile("EndTurn")),

  BEFORE_BATTLE(new HeadlessAutoSaveFileUtils().getBeforeStepAutoSaveFile("Battle")),

  AFTER_BATTLE(new HeadlessAutoSaveFileUtils().getAfterStepAutoSaveFile("Battle")),

  AFTER_COMBAT_MOVE(new HeadlessAutoSaveFileUtils().getAfterStepAutoSaveFile("CombatMove")),

  AFTER_NON_COMBAT_MOVE(new HeadlessAutoSaveFileUtils().getAfterStepAutoSaveFile("NonCombatMove"));

  private final Path path;

  HeadlessAutoSaveType(final File file) {
    path = file.toPath();
  }

  public File getFile() {
    return path.toFile();
  }
}
