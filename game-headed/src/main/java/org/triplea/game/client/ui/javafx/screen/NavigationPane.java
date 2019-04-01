package org.triplea.game.client.ui.javafx.screen;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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

    final DoubleProperty opacity = opacityProperty();
    final Node screen = screens.get(identifier);

    if (getChildren().isEmpty()) {
      fadeInScreen(screen, opacity);
    } else {
      fadeInReplacementScreen(screen, opacity);
    }
  }

  private void fadeInScreen(final Node screen, final DoubleProperty opacity) {
    setOpacity(0.0);
    getChildren().add(screen);
    final Timeline fadeIn = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
        new KeyFrame(new Duration(500), new KeyValue(opacity, 1.0)));
    fadeIn.play();
  }

  private void fadeInReplacementScreen(final Node screen, final DoubleProperty opacity) {
    final Timeline fade = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 1.0)),
        new KeyFrame(new Duration(400), t -> {
          getChildren().set(0, screen);
          final Timeline fadeIn = new Timeline(
              new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
              new KeyFrame(new Duration(400), new KeyValue(opacity, 1.0)));
          fadeIn.play();
        }, new KeyValue(opacity, 0.0)));
    fade.play();
  }
}
