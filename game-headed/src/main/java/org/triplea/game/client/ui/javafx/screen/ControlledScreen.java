package org.triplea.game.client.ui.javafx.screen;

import javafx.scene.Node;

public interface ControlledScreen<T extends ScreenController<?>> {
  void connect(T screenController);
  Node getNode();
}
