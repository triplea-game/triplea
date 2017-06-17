package org.triplea.client.ui.javafx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

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
    final Scene scene = new Scene(loader.load(), 960, 540);// TODO make those values configurable
    scene.getStylesheets().add(FxmlManager.STYLESHEET_MAIN.toString());
    mainMenu = addRootContent(new MainMenuPane(this));
    stage.setMinHeight(scene.getHeight());
    stage.setMinWidth(scene.getWidth());
    stage.setScene(scene);
    stage.getIcons().add(new Image(getClass().getResourceAsStream(FxmlManager.ICON_LOCATION.toString())));
    stage.setTitle("TripleA");
    stage.show();
  }

  public static void launch(final String... arg) {
    Application.launch(arg);
  }

  void returnToMainMenu(Node currentPane) {
    currentPane.setVisible(false);
    mainMenu.setVisible(true);
  }

  public void promptExit() {
    exitOverlay.setVisible(true);
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

  void displayLoadingScreen(final boolean bool) {
    loadingOverlay.setVisible(bool);
  }

  void setLoadingMessage(final String message) {
    progressLabel.setText(message);
    progressTooltip.setText(message);
  }
}
