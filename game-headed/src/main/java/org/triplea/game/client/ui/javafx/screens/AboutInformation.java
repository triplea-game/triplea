package org.triplea.game.client.ui.javafx.screens;

import org.triplea.awt.OpenFileUtility;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.triplea.UrlConstants;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class AboutInformation implements ControlledScreen<ScreenController<FxmlManager>> {

  @FXML
  private VBox aboutSection;

  private ScreenController<FxmlManager> screenController;

  @VisibleForTesting
  AboutInformation(final VBox aboutSection) {
    this.aboutSection = aboutSection;
  }

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
  public void connect(final ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return aboutSection;
  }

  @FXML
  @VisibleForTesting
  void back() {
    screenController.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
