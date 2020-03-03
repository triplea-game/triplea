package org.triplea.game.client.ui.javafx.screen;

import java.util.Map;

/**
 * A common interface to wrap the logic for swapping screens.
 *
 * @param <T> Type of the identifier used to reference different screens.
 */
public interface ScreenController<T> {
  default void switchScreen(T identifier) {
    switchScreen(identifier, Map.of());
  }

  void switchScreen(T identifier, Map<String, Object> data);
}
