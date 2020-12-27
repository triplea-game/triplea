package games.strategy.engine.framework;

import games.strategy.triplea.player.AbstractBasePlayer;
import java.util.ArrayList;
import java.util.Collection;
import lombok.experimental.UtilityClass;

/**
 * Adds callbacks to be run when a game is ending.
 *
 * <p>Useful for cleaning up static resources that are only needed while a game is running.
 *
 * <p>All callbacks are removed when the game ends.
 *
 * <p>Use {@link AbstractBasePlayer#stopGame()} instead if possible.
 */
@UtilityClass
@Deprecated
public class GameShutdownRegistry {

  private static final Collection<Runnable> shutdownActions = new ArrayList<>();

  public void registerShutdownAction(final Runnable shutdownAction) {
    shutdownActions.add(shutdownAction);
  }

  public void unregisterShutdownAction(final Runnable shutdownAction) {
    shutdownActions.remove(shutdownAction);
  }

  public void runShutdownActions() {
    synchronized (shutdownActions) {
      shutdownActions.forEach(Runnable::run);
      shutdownActions.clear();
    }
  }
}
