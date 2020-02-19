package org.triplea.game.client.ui.javafx.screens;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.UrlConstants;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import org.triplea.awt.OpenFileUtility;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

/** Controller class for the About Screen. */
@NoArgsConstructor
public class AboutInformation implements ControlledScreen<ScreenController<FxmlManager>> {

  @FXML private VBox aboutSection;

  private ScreenController<FxmlManager> screenController;

  @VisibleForTesting
  AboutInformation(final VBox aboutSection) {
    this.aboutSection = aboutSection;
  }

  @FXML
  @SuppressWarnings("unused")
  private void showHelp() {
    open(UrlConstants.USER_GUIDE);
  }

  private void open(final String url) {
    OpenFileUtility.openUrl(
        url, () -> new Alert(Alert.AlertType.INFORMATION, url, ButtonType.CLOSE).show());
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
