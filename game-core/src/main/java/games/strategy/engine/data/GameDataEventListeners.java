package games.strategy.engine.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.function.Consumer;

/**
 * Container object for listeners to GameDataEvent. Given a GameDataEvent, can be used to invoke
 * listeners.
 */
class GameDataEventListeners implements Consumer<GameDataEvent> {

  private final Multimap<GameDataEvent, Runnable> listeners = HashMultimap.create();

  @Override
  public void accept(final GameDataEvent gameDataEvent) {
    listeners.get(gameDataEvent).forEach(Runnable::run);
  }

  void addListener(final GameDataEvent event, final Runnable runnable) {
    listeners.put(event, runnable);
  }
}
