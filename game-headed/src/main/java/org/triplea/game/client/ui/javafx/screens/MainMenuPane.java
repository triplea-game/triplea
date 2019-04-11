package org.triplea.game.client.ui.javafx.screens;

import java.text.MessageFormat;

import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.engine.ClientContext;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Controller representing the MainMenu JavaFX implementation.
 */
public class MainMenuPane implements ControlledScreen<NavigationPane> {

  @FXML
  private Label version;

  @FXML
  private StackPane content;

  private NavigationPane navigationPane;

  @FXML
  private BorderPane root;

  @FXML
  private void initialize() {
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion().getExactVersion()));
    navigationPane = new NavigationPane();
    content.getChildren().add(0, navigationPane.getNode());

    navigationPane.registerScreen(FxmlManager.GAME_SELECTION_CONTROLS);
    navigationPane.registerScreen(FxmlManager.ABOUT_INFORMATION);
    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }


  @Override
  public void connect(final NavigationPane screenController) {
    navigationPane.setParent(screenController);
  }

  @Override
  public Node getNode() {
    return root;
  }
}
