package org.triplea.game.client.ui.javafx;

import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.engine.framework.GameRunner;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * The Main-UI-Class for the JavaFX-UI.
 * It sets up the Stage accordingly.
 */
public class TripleA extends Application {
  private static final String STYLESHEET_MAIN = "/org/triplea/game/client/ui/javafx/css/main.css";
  private static final String FONT_PATH = "/org/triplea/game/client/ui/javafx/css/fonts/1942-report.ttf";
  private static final String ICON_LOCATION = "/org/triplea/swing/ta_icon.png";

  @Override
  public void start(final Stage stage) throws Exception {
    final RootActionPane rootActionPane = new RootActionPane();
    final Scene scene = new Scene(rootActionPane);
    scene.getStylesheets().add(STYLESHEET_MAIN);

    final NavigationPane navigationPane = new NavigationPane();

    navigationPane.registerScreen(new MainMenuPane(rootActionPane));
    navigationPane.registerScreen(new SettingsPane());

    rootActionPane.setContent(navigationPane);

    navigationPane.switchScreen(MainMenuPane.class);

    setupStage(stage, scene, rootActionPane);
    // Don't invoke Swing if headless (for example in tests)
    if (!GraphicsEnvironment.isHeadless()) {
      SwingUtilities.invokeLater(GameRunner::newMainFrame);
    }
  }

  private void setupStage(final Stage stage, final Scene scene, final RootActionPane rootActionPane) {
    stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
    stage.setFullScreen(true);

    Font.loadFont(TripleA.class.getResourceAsStream(FONT_PATH), 14);
    stage.setScene(scene);
    stage.getIcons().add(new Image(getClass().getResourceAsStream(ICON_LOCATION)));
    stage.setTitle("TripleA");
    stage.show();
    stage.setOnCloseRequest(e -> {
      e.consume();
      rootActionPane.promptExit();
    });
  }
}
