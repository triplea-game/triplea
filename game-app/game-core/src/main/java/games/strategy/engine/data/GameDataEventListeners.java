package games.strategy.engine.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.function.Consumer;

/**
 * Container object for listeners to GameDataEvent. Given a GameDataEvent, can be used to invoke
 * listeners.
 */
class GameDataEventListeners implements Consumer<GameDataEvent> {

  private final Multimap<GameDataEvent, Runnable> listeners = HashMultimap.create();

  @Override
  public void accept(final GameDataEvent gameDataEvent) {
    // Create a list copy to avoid a ConcurrentModificationException
    // The list copy is a band-aid fix for: https://github.com/triplea-game/triplea/issues/7588
    List.copyOf(listeners.get(gameDataEvent)).forEach(Runnable::run);
  }

  void addListener(final GameDataEvent event, final Runnable runnable) {
    listeners.put(event, runnable);
  }
}
