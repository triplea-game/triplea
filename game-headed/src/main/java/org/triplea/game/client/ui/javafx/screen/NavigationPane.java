package org.triplea.game.client.ui.javafx.screen;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class NavigationPane extends StackPane implements ScreenController<Class<? extends Node>> {
  private final Map<Class<? extends Node>, Node> screens = new HashMap<>();

  public void registerScreen(final Node screen) {
    Preconditions.checkState(Platform.isFxApplicationThread());
    Preconditions.checkNotNull(screen);
    screens.put(screen.getClass(), screen);
  }

  @Override
  public void switchScreen(final Class<? extends Node> identifier) {
    Preconditions.checkNotNull(identifier);
    Preconditions.checkArgument(screens.containsKey(identifier), "Screen of Type " + identifier + " not present");

    final Node screen = screens.get(identifier);

    if (getChildren().isEmpty()) {
      getChildren().add(screen);
    } else {
      getChildren().set(0, screen);
    }
  }
}
