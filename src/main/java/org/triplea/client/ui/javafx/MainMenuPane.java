package org.triplea.client.ui.javafx;

import java.io.IOException;
import java.text.MessageFormat;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.UrlConstants;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class MainMenuPane extends BorderPane {

  private final TripleA triplea;
  private final DownloadPane downloadPane;
  private final SettingsPane settingsPane;

  @FXML
  private Text loggedInText;

  @FXML
  private HBox loginForm;

  @FXML
  private TextField username;

  @FXML
  private PasswordField password;

  @FXML
  private Button buttonBack;

  @FXML
  private Text version;

  @FXML
  private VBox aboutSection;

  @FXML
  private HBox gameOptions;

  @FXML
  private VBox mainOptions;

  /**
   * @param triplea The root pane.
   * @throws IOException If the FXML file is not present.
   */
  public MainMenuPane(final TripleA triplea) throws IOException {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.MAIN_MANU_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.triplea = triplea;
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion().getFullVersion()));
    downloadPane = triplea.addRootContent(new DownloadPane(triplea));
    downloadPane.setVisible(false);
    settingsPane = triplea.addRootContent(new SettingsPane(triplea));
    settingsPane.setVisible(false);
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
    triplea.open(UrlConstants.GITHUB_HELP.toString());
  }

  @FXML
  private void showRuleBook() {
    triplea.open(UrlConstants.RULE_BOOK.toString());
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
  private void showPbfPbemMenu() {}

  @FXML
  private void showPlayOptions() {
    mainOptions.setVisible(false);
    gameOptions.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showDownloadMenu() {
    setVisible(false);
    downloadPane.setVisible(true);
  }

  @FXML
  private void showSettingsMenu() {
    setVisible(false);
    settingsPane.setVisible(true);
  }

  @FXML
  private void showAboutSection() {
    mainOptions.setVisible(false);
    aboutSection.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showExitConfirmDialog() {
    triplea.promptExit();
  }

  @FXML
  private void startHover(final MouseEvent e) {}

  @FXML
  private void endHover(final MouseEvent e) {}
}
