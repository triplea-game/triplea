package org.triplea.game.client.ui.javafx.screen;

import java.util.Map;
import javafx.scene.Node;

/**
 * Interface that all Controllers should implement that represent a screen that can be swapped out
 * by another one.
 *
 * @param <T> The Type of the {@link ScreenController}.
 */
public interface ControlledScreen<T extends ScreenController<?>> {

  /**
   * A method being called by the {@link ScreenController} when this Screen is getting registered.
   *
   * @param screenController The calling {@link ScreenController} instance.
   */
  void connect(T screenController);

  /** A common method to retrieve the root Node to attach it to the Scene Graph. */
  Node getNode();

  /**
   * Method being called whenever this screen is being shown.
   *
   * @param data Data that is being passed by another screen, validated according to {@link
   *     #getValidTypes()}.
   */
  default void onShow(Map<String, Object> data) {}

  /** Returns a mapping of valid types that are being passed to a certain screen. */
  default Map<String, Class<?>> getValidTypes() {
    return Map.of();
  }
}
