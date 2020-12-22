package games.strategy.engine.framework;

import java.util.ArrayList;
import java.util.Collection;
import lombok.experimental.UtilityClass;

@UtilityClass
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
