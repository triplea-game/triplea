package org.triplea.game.client.ui.javafx.screens;

import java.util.function.Function;

import javax.swing.SwingUtilities;

import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

public class MainMenuControls implements ControlledScreen<NavigationPane> {

  @FXML
  private VBox mainOptions;

  private NavigationPane navigationPane;

  @FXML
  private void initialize() {
    mainOptions.lookupAll(".button").forEach(node -> {
      final Function<Node, NumberBinding> hoverBinding = n -> Bindings.when(n.hoverProperty()).then(-10).otherwise(0);
      final NumberBinding numberBinding = hoverBinding.apply(node);
      node.translateYProperty().bind(numberBinding.multiply(-1));
      node.getParent().translateYProperty().bind(!mainOptions.equals(node.getParent().getParent())
          ? Bindings.add(numberBinding,
          hoverBinding.apply(node.getParent().getParent().getChildrenUnmodifiable().get(0)).multiply(-1))
          : numberBinding);
    });
  }

  @FXML
  private void showPlayOptions() {
    navigationPane.switchScreen(FxmlManager.GAME_SELECTION_CONTROLS);
  }

  @FXML
  private void showDownloadMenu() {
    SwingUtilities.invokeLater(DownloadMapsWindow::showDownloadMapsWindow);
  }

  @FXML
  private void showSettingsMenu() {
    navigationPane.switchScreen(FxmlManager.SETTINGS_PANE);
  }

  @FXML
  private void showAboutSection() {
    navigationPane.switchScreen(FxmlManager.ABOUT_INFORMATION);
  }

  @FXML
  private void showExitConfirmDialog() {
    ((RootActionPane) mainOptions.getScene().getWindow().getUserData()).promptExit();
  }

  @Override
  public void connect(final NavigationPane screenController) {
    navigationPane = screenController;
  }

  @Override
  public Node getNode() {
    return mainOptions;
  }
}
