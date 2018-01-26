package org.triplea.client.ui.javafx.controls;

import javafx.animation.FillTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

public class ToggleSwitch extends Region {
  private final BooleanProperty switchedOn = new SimpleBooleanProperty(false);

  private final TranslateTransition translateAnimation = new TranslateTransition(Duration.seconds(0.25));
  private final FillTransition fillAnimation = new FillTransition(Duration.seconds(0.25));

  private final ParallelTransition animation = new ParallelTransition(translateAnimation, fillAnimation);

  public BooleanProperty switchedOnProperty() {
    return switchedOn;
  }

  public ToggleSwitch(final boolean initialState) {
    this(initialState, 50, 25);
  }

  ToggleSwitch(final boolean initialState, final double width, final double height) {
    final Rectangle background = new Rectangle(width, height);
    background.setArcWidth(height);
    background.setArcHeight(height);
    background.setFill(Color.web("#B8242B"));
    background.setStroke(Color.LIGHTGRAY);

    final Circle trigger = new Circle(height / 2);
    trigger.setCenterX(height / 2);
    trigger.setCenterY(height / 2);
    trigger.setFill(Color.WHITE);
    trigger.setStroke(Color.LIGHTGRAY);

    final DropShadow shadow = new DropShadow();
    shadow.setRadius(2);
    trigger.setEffect(shadow);

    translateAnimation.setNode(trigger);
    fillAnimation.setShape(background);

    translateAnimation.setFromX(0);
    translateAnimation.setToX(width - height);
    fillAnimation.setFromValue(Color.web("#B8242B"));
    fillAnimation.setToValue(Color.web("#52602F"));

    getChildren().addAll(background, trigger);

    switchedOn.addListener((obs, oldState, newState) -> {
      animation.setRate(newState ? 1 : -1);
      animation.play();
    });

    setOnMouseClicked(event -> switchedOn.set(!switchedOn.get()));
    switchedOn.set(initialState);
  }
}
