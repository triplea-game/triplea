package org.triplea.game.client.ui.javafx.screens;

import com.google.common.annotations.VisibleForTesting;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import lombok.NoArgsConstructor;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

/** Controller class for the Game Type selection screen. */
@NoArgsConstructor
public class GameSelectionControls implements ControlledScreen<ScreenController<FxmlManager>> {

  @FXML private BorderPane gameOptions;

  private ScreenController<FxmlManager> screenController;

  @VisibleForTesting
  GameSelectionControls(final BorderPane gameOptions) {
    this.gameOptions = gameOptions;
  }

  @SuppressWarnings("unused")
  @FXML
  private void showLobbyMenu() {}

  @SuppressWarnings("unused")
  @FXML
  void showLocalGameMenu() {
    screenController.switchScreen(FxmlManager.MAP_SELECTION);
  }

  @SuppressWarnings("unused")
  @FXML
  private void showHostNetworkGameMenu() {}

  @SuppressWarnings("unused")
  @FXML
  private void showJoinNetworkGameMenu() {}

  @SuppressWarnings("unused")
  @FXML
  private void showPlayByForumMenu() {}

  @SuppressWarnings("unused")
  @FXML
  private void showPlayByEmailMenu() {}

  @Override
  public void connect(final ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return gameOptions;
  }

  @FXML
  @VisibleForTesting
  void back() {
    screenController.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
