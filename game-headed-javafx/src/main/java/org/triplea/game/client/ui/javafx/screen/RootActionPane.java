package org.triplea.game.client.ui.javafx.screen;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.GameRunner;
import java.awt.GraphicsEnvironment;
import java.util.Map;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javax.swing.SwingUtilities;
import org.triplea.game.client.ui.javafx.screen.RootActionPane.Screens;

/**
 * Root JavaFX Panel Controller that supports a variety of options such as prompting the user to
 * exit the Application.
 */
public class RootActionPane implements ScreenController<Screens> {

  enum Screens {
    EXIT,

    LOADING,

    CONTENT
  }

  @FXML private StackPane root;

  @FXML private VBox loadingOverlay;

  @FXML private VBox exitOverlay;

  @FXML private VBox exitFrame;

  public void setContent(final Node node) {
    Preconditions.checkNotNull(node);
    Preconditions.checkState(Platform.isFxApplicationThread());

    if (root.getChildren().size() == Screens.values().length) {
      root.getChildren().set(0, node);
    } else {
      root.getChildren().add(0, node);
    }
  }

  public void setLoadingOverlay(final boolean loading) {
    switchScreen(loading ? Screens.LOADING : Screens.CONTENT);
  }

  public void promptExit() {
    switchScreen(Screens.EXIT);
  }

  @FXML
  @SuppressWarnings("unused")
  private void hideExitConfirm() {
    switchScreen(Screens.CONTENT);
  }

  @FXML
  @SuppressWarnings({"static-method", "unused"})
  private void exit() {
    Platform.exit();
    if (!GraphicsEnvironment.isHeadless()) {
      SwingUtilities.invokeLater(GameRunner::exitGameIfFinished);
    }
  }

  @Override
  public void switchScreen(final Screens identifier, final Map<String, Object> data) {
    Preconditions.checkNotNull(identifier);

    switch (identifier) {
      case EXIT:
        final Timeline fadeIn = getAnimation(exitFrame);
        fadeIn.play();
        exitOverlay.setVisible(true);
        break;
      case LOADING:
        loadingOverlay.setVisible(true);
        break;
      case CONTENT:
        exitOverlay.setVisible(false);
        loadingOverlay.setVisible(false);
        if (root.getChildren().size() == Screens.values().length) {
          root.getChildren().get(0).setVisible(true);
        }
        break;
      default:
        throw new AssertionError("Invalid Switch Case");
    }
  }

  private static Timeline getAnimation(final Node node) {
    return new Timeline(
        new KeyFrame(
            Duration.ZERO,
            new KeyValue(node.scaleXProperty(), 0.0),
            new KeyValue(node.scaleYProperty(), 0.0)),
        new KeyFrame(new Duration(100), new KeyValue(node.scaleYProperty(), 1.1)),
        new KeyFrame(new Duration(200), new KeyValue(node.scaleXProperty(), 0.4)),
        new KeyFrame(
            new Duration(300),
            new KeyValue(node.scaleXProperty(), 1.0),
            new KeyValue(node.scaleYProperty(), 1.0)));
  }
}
