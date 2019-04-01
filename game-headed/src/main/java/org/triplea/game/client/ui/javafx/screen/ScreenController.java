package org.triplea.game.client.ui.javafx.screen;

import javafx.scene.Node;

public interface ScreenController<T> {
  void switchScreen(T identifier);
}
