package org.triplea.game.client.ui.javafx.screen;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.EnumMap;
import java.util.Map;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

/**
 * The default implementation of {@link ScreenController}. Using this class enables to swap screens
 * with a rather flat call tree by using the class name as the identifier. Make sure to register
 * Screens before using them.
 */
public class NavigationPane implements ScreenController<FxmlManager> {
  private final Pane root;
  private final Map<FxmlManager, ControlledScreen<NavigationPane>> screens =
      new EnumMap<>(FxmlManager.class);
  private ScreenController<FxmlManager> parent;

  public NavigationPane() {
    this(new StackPane());
  }

  @VisibleForTesting
  NavigationPane(final Pane root) {
    this.root = root;
  }

  @VisibleForTesting
  void registerScreen(final FxmlManager manager, final ControlledScreen<NavigationPane> screen) {
    Preconditions.checkNotNull(screen);
    screens.put(manager, screen);
    screen.connect(this);
  }

  public void registerScreen(final FxmlManager manager) {
    Preconditions.checkNotNull(manager);
    registerScreen(
        manager, manager.<ControlledScreen<NavigationPane>, Object>load().getController());
  }

  @Override
  public void switchScreen(final FxmlManager identifier, final Map<String, Object> data) {
    Preconditions.checkNotNull(identifier);

    // Pass request to parent if we can't handle it.
    if (!screens.containsKey(identifier)) {
      if (parent != null) {
        parent.switchScreen(identifier);
        return;
      }
      throw new IllegalArgumentException("Screen of Type " + identifier + " not present");
    }

    final Node screen = screens.get(identifier).getNode();

    if (root.getChildren().isEmpty()) {
      root.getChildren().add(screen);
    } else {
      root.getChildren().set(0, screen);
    }
    final ControlledScreen<NavigationPane> controlledScreen = screens.get(identifier);
    checkTypes(data, controlledScreen.getValidTypes());
    controlledScreen.onShow(data);
  }

  private static void checkTypes(
      final Map<String, Object> data, final Map<String, Class<?>> types) {
    for (final var entry : data.entrySet()) {
      final String key = entry.getKey();
      final Class<?> requiredClass = types.getOrDefault(key, Void.class);
      if (entry.getValue() != null && !requiredClass.isInstance(entry.getValue())) {
        final String errorString =
            String.format(
                "Key '%s' has invalid type '%s', expected '%s'",
                key, entry.getValue().getClass(), requiredClass);
        throw new IllegalArgumentException(errorString);
      }
    }
  }

  public void setParent(final ScreenController<FxmlManager> parent) {
    this.parent = parent;
  }

  public Node getNode() {
    return root;
  }
}
