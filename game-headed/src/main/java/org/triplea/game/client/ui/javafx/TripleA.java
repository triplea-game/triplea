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
 * The root of everything.
 */
public class TripleA extends Application {

  @Override
  public void start(final Stage stage) throws Exception {
    final RootActionPane rootActionPane = new RootActionPane();
    final Scene scene = new Scene(rootActionPane);
    scene.getStylesheets().add(FxmlManager.STYLESHEET_MAIN.toString());

    final NavigationPane navigationPane = new NavigationPane();

    navigationPane.registerScreen(new MainMenuPane(rootActionPane, navigationPane));
    navigationPane.registerScreen(new SettingsPane(navigationPane));

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

    Font.loadFont(TripleA.class.getResourceAsStream(FxmlManager.FONT_PATH.toString()), 14);
    stage.setScene(scene);
    stage.getIcons().add(new Image(getClass().getResourceAsStream(FxmlManager.ICON_LOCATION.toString())));
    stage.setTitle("TripleA");
    stage.show();
    stage.setOnCloseRequest(e -> {
      e.consume();
      rootActionPane.promptExit();
    });
  }
}
