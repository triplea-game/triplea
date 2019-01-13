package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;

/**
 * Abstract class for launching a game.
 */
public abstract class AbstractLauncher<T> implements ILauncher {
  @Override
  public void launch(final Component parent) {
    final T result = loadGame(parent);
    new Thread(() -> launchInternal(parent, result)).start();
  }

  abstract T loadGame(Component parent);

  abstract void launchInternal(Component parent, T data);
}
