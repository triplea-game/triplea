package org.triplea.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;

import games.strategy.debug.ClientLogger;
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

public class TripleaMainMenu extends BorderPane {

  private final TripleA triplea;
  private final TripleaDownload downloadPane;
  private final TripleaSettings settingsPane;

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
   * @param triplea The root pane
   * @throws IOException If the FXML file is not present
   */
  public TripleaMainMenu(final TripleA triplea) throws IOException {
    final FXMLLoader loader = TripleA.getLoader(getClass().getResource("./fxml/TripleAMainMenu.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.triplea = triplea;
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion().getFullVersion()));
    downloadPane = triplea.addRootContent(new TripleaDownload(triplea));
    downloadPane.setVisible(false);
    settingsPane = triplea.addRootContent(new TripleaSettings(triplea));
    settingsPane.setVisible(false);
  }

  @FXML
  private void login() {

  }

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
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().browse(new URI(UrlConstants.GITHUB_HELP.toString()));
      } catch (IOException | URISyntaxException e) {
        ClientLogger.logQuietly("Error while trying to open URL", e);
      }
    } else {
      // TODO Desktop API is not supported
    }
  }

  @FXML
  private void showRuleBook() {
    if (Desktop.isDesktopSupported()) {
      try {
        Desktop.getDesktop().browse(new URI(UrlConstants.RULE_BOOK.toString()));
      } catch (IOException | URISyntaxException e) {
        ClientLogger.logQuietly("Error while trying to open URL", e);
      }
    } else {
      // TODO Desktop API is not supported
    }
  }

  @FXML
  private void showLobbyMenu() {

  }

  @FXML
  private void showLocalGameMenu() {

  }

  @FXML
  private void showHostNetworkGameMenu() {

  }

  @FXML
  private void showJoinNetworkGameMenu() {

  }

  @FXML
  private void showPbfPbemMenu() {

  }

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
