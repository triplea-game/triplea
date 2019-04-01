package org.triplea.game.client.ui.javafx.screen;

import javafx.scene.Node;

public interface ScreenController {
  void switchScreen(Class<? extends Node> identifier);
}
