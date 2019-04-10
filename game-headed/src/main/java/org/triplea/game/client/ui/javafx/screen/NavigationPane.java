package org.triplea.game.client.ui.javafx.screen;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * The default implementation of {@link ScreenController}.
 * Using this class enables to swap screens with a rather flat
 * call tree by using the class name as the identifier.
 * Make sure to register Screens before using them.
 */
public class NavigationPane extends StackPane implements ScreenController<Class<? extends ControlledScreen<NavigationPane>>> {
  private final Map<Class<? extends ControlledScreen<NavigationPane>>, Node> screens = new HashMap<>();

  public void registerScreen(final ControlledScreen<NavigationPane> screen) {
    Preconditions.checkState(Platform.isFxApplicationThread());
    Preconditions.checkNotNull(screen);
    screens.put(unchecked(screen.getClass()), screen.getNode());
    screen.connect(this);
  }

  /**
   * This method exists because generics in Java are terrible to work with,
   * you should probably not be using it.
   */
  @SuppressWarnings("unchecked")
  private static Class<? extends ControlledScreen<NavigationPane>> unchecked(final Class<?> clazz) {
    return (Class<? extends ControlledScreen<NavigationPane>>) clazz;
  }

  @Override
  public void switchScreen(final Class<? extends ControlledScreen<NavigationPane>> identifier) {
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
