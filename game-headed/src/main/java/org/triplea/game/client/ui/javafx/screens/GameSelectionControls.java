package org.triplea.game.client.ui.javafx.screens;

import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

public class GameSelectionControls implements ControlledScreen<NavigationPane> {

  @FXML
  private BorderPane gameOptions;

  private NavigationPane screenController;

  @FXML
  private void showLobbyMenu() {}

  @FXML
  private void showLocalGameMenu() {}

  @FXML
  private void showHostNetworkGameMenu() {}

  @FXML
  private void showJoinNetworkGameMenu() {}

  @FXML
  private void showPlayByForumMenu() {}

  @FXML
  private void showPlayByEmailMenu() {}

  @Override
  public void connect(final NavigationPane screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return gameOptions;
  }

  @FXML
  private void back() {
    screenController.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
