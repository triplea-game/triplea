package org.triplea.game.client.ui.javafx.screen;

import java.util.Map;

import com.google.common.base.Preconditions;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public abstract class AbstractNavigationPane<T> extends StackPane implements ScreenController<T> {

  final Map<T, Node> screens;

  AbstractNavigationPane(final Map<T, Node> screens) {
    this.screens = screens;
  }


  @Override
  public void switchScreen(final T identifier) {
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

  void fadeInScreen(final Node screen, final DoubleProperty opacity) {
    setOpacity(0.0);
    getChildren().add(screen);
    final Timeline fadeIn = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
        new KeyFrame(new Duration(2500), new KeyValue(opacity, 1.0)));
    fadeIn.play();
  }

  private void fadeInReplacementScreen(final Node screen, final DoubleProperty opacity) {
    final Timeline fade = new Timeline(
        new KeyFrame(Duration.ZERO, new KeyValue(opacity, 1.0)),
        new KeyFrame(new Duration(1000), t -> {
          getChildren().set(0, screen);
          final Timeline fadeIn = new Timeline(
              new KeyFrame(Duration.ZERO, new KeyValue(opacity, 0.0)),
              new KeyFrame(new Duration(800), new KeyValue(opacity, 1.0)));
          fadeIn.play();
        }, new KeyValue(opacity, 0.0)));
    fade.play();
  }
}
