package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;

/**
 * Abstract class for launching a game.
 */
public abstract class AbstractLauncher<T> implements ILauncher {
  protected final GameData gameData;
  protected final GameSelectorModel gameSelectorModel;
  protected final boolean headless;

  AbstractLauncher(final GameSelectorModel gameSelectorModel) {
    this(gameSelectorModel, false);
  }

  AbstractLauncher(final GameSelectorModel gameSelectorModel, final boolean headless) {
    this.headless = headless;
    this.gameSelectorModel = gameSelectorModel;
    gameData = gameSelectorModel.getGameData();
  }

  @Override
  public void launch(final Component parent) {
    final T result = loadGame(parent);
    new Thread(() -> launchInternal(parent, result)).start();
  }

  abstract T loadGame(Component parent);

  abstract void launchInternal(Component parent, T data);
}
