package org.triplea.game.client.ui.javafx.screen;

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
}
