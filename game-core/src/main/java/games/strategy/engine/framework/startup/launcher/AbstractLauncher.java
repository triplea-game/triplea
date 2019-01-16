package games.strategy.engine.framework.startup.launcher;

import java.awt.Component;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Abstract class for launching a game.
 *
 * @param <T> The type of object that gets returned by {@link #loadGame(Component)}
 *        and is required by {@link #launchInternal(Component, Object)}.
 */
public abstract class AbstractLauncher<T> implements ILauncher {
  @Override
  public void launch(final Component parent) {
    final Optional<T> result = loadGame(parent);
    new Thread(() -> launchInternal(parent, result.orElse(null))).start();
  }

  abstract Optional<T> loadGame(Component parent);

  abstract void launchInternal(Component parent, @Nullable T data);
}
