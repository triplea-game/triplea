package org.triplea.client.ui.javafx;

import java.io.File;

import org.triplea.client.ui.javafx.util.FxmlManager;

import games.strategy.net.OpenFileUtility;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * The Main-UI-Class for the JavaFX-UI.
 * The root of everything.
 */
public class TripleA extends Application {

  @FXML
  private VBox loadingOverlay;

  @FXML
  private VBox exitOverlay;

  @FXML
  private Tooltip progressTooltip;

  @FXML
  private Label progressLabel;

  @FXML
  private StackPane rootPane;

  private MainMenuPane mainMenu;

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.ROOT_CONTAINER.toString()));
    loader.setController(this);
    final Scene scene = new Scene(loader.load());
    scene.getStylesheets().add(FxmlManager.STYLESHEET_MAIN.toString());
    mainMenu = addRootContent(new MainMenuPane(this));
    setupStage(stage, scene);
  }

  private void setupStage(final Stage stage, final Scene scene) {
    stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    stage.setFullScreen(true);

    Font.loadFont(TripleA.class.getResourceAsStream(FxmlManager.FONT_PATH.toString()), 14);
    stage.setScene(scene);
    stage.getIcons().add(new Image(getClass().getResourceAsStream(FxmlManager.ICON_LOCATION.toString())));
    stage.setTitle("TripleA");
    stage.show();
  }

  void returnToMainMenu(final Node currentPane) {
    currentPane.setVisible(false);
    mainMenu.setVisible(true);
  }

  public void promptExit() {
    exitOverlay.setVisible(true);
  }

  public void open(final File file) {
    OpenFileUtility.openFile(file, () -> showDesktopApiNotSupportedError(file.getAbsolutePath()));
  }

  public void open(final String url) {
    OpenFileUtility.openUrl(url, () -> showDesktopApiNotSupportedError(url));
  }

  private void showDesktopApiNotSupportedError(final String path) {
    showErrorMessage("Desktop API not supported", "Could not open '" + path + "' automatically!");
  }

  public void showErrorMessage(final String title, final String message) {
    // TODO
  }

  <T extends Node> T addRootContent(final T node) {
    rootPane.getChildren().add(node);
    return node;
  }

  @FXML
  private void hideExitConfirm() {
    exitOverlay.setVisible(false);
  }

  @FXML
  private void exit() {
    Platform.exit();
  }

  public void displayLoadingScreen(final boolean bool) {
    loadingOverlay.setVisible(bool);
  }

  public void setLoadingMessage(final String message) {
    progressLabel.setText(message);
    progressTooltip.setText(message);
  }
}
