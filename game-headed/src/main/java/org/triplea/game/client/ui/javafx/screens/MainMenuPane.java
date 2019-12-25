package org.triplea.game.client.ui.javafx.screens;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.ClientContext;
import java.text.MessageFormat;
import java.util.function.Supplier;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

/** Controller representing the MainMenu JavaFX implementation. */
public class MainMenuPane implements ControlledScreen<NavigationPane> {

  @FXML private Label version;

  @FXML private StackPane content;

  private NavigationPane navigationPane;

  @FXML private BorderPane root;

  private final Supplier<NavigationPane> constructor;

  @SuppressWarnings("unused") // Invoked reflectively by FXML
  public MainMenuPane() {
    constructor = NavigationPane::new;
  }

  @VisibleForTesting
  MainMenuPane(
      final Supplier<NavigationPane> constructor,
      final BorderPane root,
      final StackPane content,
      final Label version) {
    this.constructor = constructor;
    this.root = root;
    this.content = content;
    this.version = version;
  }

  @FXML
  @VisibleForTesting
  void initialize() {
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion()));
    navigationPane = constructor.get();
    content.getChildren().add(0, navigationPane.getNode());

    navigationPane.registerScreen(FxmlManager.GAME_SELECTION_CONTROLS);
    navigationPane.registerScreen(FxmlManager.ABOUT_INFORMATION);
    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS);
    navigationPane.registerScreen(FxmlManager.MAP_SELECTION);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }

  @Override
  public void connect(final NavigationPane screenController) {
    navigationPane.setParent(screenController);
  }

  @Override
  public Node getNode() {
    return root;
  }
}
