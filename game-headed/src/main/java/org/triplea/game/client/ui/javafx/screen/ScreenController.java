package org.triplea.game.client.ui.javafx.screen;

/**
 * A common interface to wrap the logic for swapping screens.
 *
 * @param <T> Type of the identifier used to reference different screens.
 */
public interface ScreenController<T> {
  void switchScreen(T identifier);
}
