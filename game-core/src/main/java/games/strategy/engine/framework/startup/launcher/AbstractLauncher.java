package games.strategy.engine.framework.startup.launcher;

import java.util.Optional;
import javax.annotation.Nullable;

/**
 * Abstract class for launching a game.
 *
 * @param <T> The type of object that gets returned by {@link #loadGame()} and is required by {@link
 *     #launchInternal(Object)}.
 */
public abstract class AbstractLauncher<T> implements ILauncher {
  @Override
  public void launch() {
    final Optional<T> result = loadGame();
    new Thread(() -> launchInternal(result.orElse(null))).start();
  }

  abstract Optional<T> loadGame();

  abstract void launchInternal(@Nullable T data);
}
