package games.strategy.engine.framework.startup.launcher;

import java.io.File;

import games.strategy.engine.framework.ServerGame;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.ServerModel;

public interface LaunchAction {
  void handleGameInterruption(final GameSelectorModel gameSelectorModel, final ServerModel serverModel);
  void onGameInterrupt();
  void onEnd(final String message);
  boolean isHeadless(); // Intermediate solution, remove when no longer necessary
  File getAutoSaveFile();
  void onLaunch(final ServerGame serverGame);
}
