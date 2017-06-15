package org.triplea.ui;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

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

  private TripleaMainMenu mainMenu;

  @Override
  public void start(final Stage stage) throws Exception {
    final FXMLLoader loader = getLoader(getClass().getResource("./fxml/TripleAMain.fxml"));
    loader.setController(this);
    final Scene scene = new Scene(loader.load(), 960, 540);
    scene.getStylesheets().add("org/triplea/ui/css/main.css");
    final TripleaMainMenu mainMenu = addRootContent(new TripleaMainMenu(this));
    this.mainMenu = mainMenu;
    stage.setMinHeight(scene.getHeight());
    stage.setMinWidth(scene.getWidth());
    stage.setScene(scene);
    stage.getIcons().add(new Image(getClass().getResourceAsStream("/games/strategy/engine/framework/ta_icon.png")));
    stage.setTitle("TripleA");
    stage.show();
    stage.setOnCloseRequest(e -> System.exit(0));
  }

  public static void launch(final String... arg) {
    Application.launch(arg);
  }

  /**
   * Simplified way of getting an {@link FXMLLoader} with the default settings for TripleA.
   * @param location The FXML File to load
   * @return An FXMLLoader object
   */
  public static FXMLLoader getLoader(final URL location) {
    final FXMLLoader loader = new FXMLLoader();
    loader.setLocation(location);
    loader.setResources(ResourceBundle.getBundle("org.triplea.ui.lang.TripleA", new Locale("en", "US")));
    return loader;
  }

  public TripleaMainMenu getMainMenu() {
    return mainMenu;
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
    System.exit(0);
  }

  public void displayLoadingScreen(final boolean bool) {
    loadingOverlay.setVisible(bool);
  }

  public void setLoadingMessage(final String message) {
    progressLabel.setText(message);
    progressTooltip.setText(message);
  }
}
