package org.triplea.game.client.ui.javafx.screens;

import org.triplea.awt.OpenFileUtility;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.triplea.UrlConstants;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;

public class AboutInformation implements ControlledScreen<NavigationPane> {

  @FXML
  private VBox aboutSection;

  private NavigationPane screenController;

  @FXML
  private void showHelp() {
    open(UrlConstants.GITHUB_HELP);
  }

  @FXML
  private void showRuleBook() {
    open(UrlConstants.RULE_BOOK);
  }

  private void open(final String url) {
    OpenFileUtility.openUrl(url, () -> new Alert(Alert.AlertType.INFORMATION, url, ButtonType.CLOSE).show());
  }

  @Override
  public void connect(final NavigationPane screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return aboutSection;
  }

  @FXML
  private void back() {
    screenController.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
