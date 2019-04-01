package org.triplea.game.client.ui.javafx.screen;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.triplea.game.client.ui.javafx.screen.RootActionPane.Screens;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import com.google.common.base.Preconditions;

import games.strategy.engine.framework.GameRunner;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class RootActionPane extends StackPane implements ScreenController<Screens> {

  enum Screens {
    EXIT,

    LOADING,

    CONTENT
  }

  @FXML
  private StackPane rootPane;

  @FXML
  private VBox loadingOverlay;

  @FXML
  private VBox exitOverlay;



  public RootActionPane() throws Exception {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.ROOT_CONTAINER.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
  }

  public void setContent(final Node node) {
    Preconditions.checkNotNull(node);
    Preconditions.checkState(Platform.isFxApplicationThread());

    rootPane.getChildren().clear();
    rootPane.getChildren().add(node);
  }

  public void setLoadingOverlay(final boolean loading) {
    switchScreen(loading ? Screens.LOADING : Screens.CONTENT);
  }

  public void promptExit() {
    switchScreen(Screens.EXIT);
  }

  @FXML
  private void hideExitConfirm() {
    switchScreen(Screens.CONTENT);
  }

  @FXML
  @SuppressWarnings("static-method")
  private void exit() {
    Platform.exit();
    if (!GraphicsEnvironment.isHeadless()) {
      SwingUtilities.invokeLater(GameRunner::exitGameIfFinished);
    }
  }

  @Override
  public void switchScreen(final Screens identifier) {
    Preconditions.checkNotNull(identifier);

    switch (identifier) {
      case EXIT:
        exitOverlay.setVisible(true);
        break;
      case LOADING:
        loadingOverlay.setVisible(true);
        break;
      case CONTENT:
        exitOverlay.setVisible(false);
        loadingOverlay.setVisible(false);
        rootPane.setVisible(true);
        break;
    }
  }
}
