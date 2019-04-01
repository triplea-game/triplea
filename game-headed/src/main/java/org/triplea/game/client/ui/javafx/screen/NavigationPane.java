package org.triplea.game.client.ui.javafx.screen;

import java.util.HashMap;

import com.google.common.base.Preconditions;

import javafx.application.Platform;
import javafx.scene.Node;

public class NavigationPane extends AbstractNavigationPane<Class<? extends Node>> {
  public NavigationPane() {
    super(new HashMap<>());
  }

  public void registerScreen(final Node screen) {
    Preconditions.checkState(Platform.isFxApplicationThread());
    Preconditions.checkNotNull(screen);
    screens.put(screen.getClass(), screen);
  }
}
