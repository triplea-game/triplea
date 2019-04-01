package org.triplea.game.client.ui.javafx;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.function.Function;

import javax.swing.SwingUtilities;

import org.triplea.awt.OpenFileUtility;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.triplea.UrlConstants;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.java.Log;

@Log
class MainMenuPane extends BorderPane {

  private final RootActionPane actionPane;
  private final ScreenController<Class<? extends Node>> screenController;

  @FXML
  private Label loggedIn;

  @FXML
  private HBox loginForm;

  @FXML
  private TextField username;

  @FXML
  private PasswordField password;

  @FXML
  private Button buttonBack;

  @FXML
  private Label version;

  @FXML
  private VBox aboutSection;

  @FXML
  private HBox gameOptions;

  @FXML
  private VBox mainOptions;

  /**
   * Initializes a new instance of the MainMenuPane class.
   *
   * @param actionPane The root pane.
   * @throws IOException If the FXML file is not present.
   */
  MainMenuPane(final RootActionPane actionPane, final ScreenController<Class<? extends Node>> screenController) throws IOException {
    this.actionPane = actionPane;
    this.screenController = screenController;
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.MAIN_MENU_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion().getExactVersion()));
    applyFileSelectionAnimation();
  }

  private void applyFileSelectionAnimation() {
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
  private void login() {}

  @FXML
  private void showLastMenu() {
    // TODO check which menu we are in
    aboutSection.setVisible(false);
    gameOptions.setVisible(false);
    buttonBack.setVisible(false);
    mainOptions.setVisible(true);
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
    OpenFileUtility.openUrl(url, () -> log.warning("Desktop API not supported. Could not open " + url));
  }

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

  @FXML
  private void showPlayOptions() {
    mainOptions.setVisible(false);
    gameOptions.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showDownloadMenu() {
    SwingUtilities.invokeLater(DownloadMapsWindow::showDownloadMapsWindow);
  }

  @FXML
  private void showSettingsMenu() {
    screenController.switchScreen(SettingsPane.class);
  }

  @FXML
  private void showAboutSection() {
    mainOptions.setVisible(false);
    aboutSection.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showExitConfirmDialog() {
    actionPane.promptExit();
  }

  @FXML
  private void startHover(@SuppressWarnings("unused") final MouseEvent e) {}

  @FXML
  private void endHover(@SuppressWarnings("unused") final MouseEvent e) {}
}
