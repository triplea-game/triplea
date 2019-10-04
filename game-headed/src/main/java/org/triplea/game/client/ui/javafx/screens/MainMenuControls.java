package org.triplea.game.client.ui.javafx.screens;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import java.util.function.Function;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javax.swing.SwingUtilities;
import lombok.NoArgsConstructor;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

/** Controller class for the main screen navigation menu. */
@NoArgsConstructor
public class MainMenuControls implements ControlledScreen<ScreenController<FxmlManager>> {

  @FXML private VBox mainOptions;

  private ScreenController<FxmlManager> screenController;

  @VisibleForTesting
  MainMenuControls(final VBox mainOptions) {
    this.mainOptions = mainOptions;
  }

  @FXML
  @VisibleForTesting
  void initialize() {
    mainOptions
        .lookupAll(".button")
        .forEach(
            node -> {
              final Function<Node, NumberBinding> hoverBinding =
                  n -> Bindings.when(n.hoverProperty()).then(-10).otherwise(0);
              final NumberBinding numberBinding = hoverBinding.apply(node);
              node.translateYProperty().bind(numberBinding.multiply(-1));
              node.getParent()
                  .translateYProperty()
                  .bind(
                      !mainOptions.equals(node.getParent().getParent())
                          ? Bindings.add(
                              numberBinding,
                              hoverBinding
                                  .apply(
                                      node.getParent().getParent().getChildrenUnmodifiable().get(0))
                                  .multiply(-1))
                          : numberBinding);
            });
  }

  @FXML
  @VisibleForTesting
  void showPlayOptions() {
    screenController.switchScreen(FxmlManager.GAME_SELECTION_CONTROLS);
  }

  @FXML
  @VisibleForTesting
  void showDownloadMenu() {
    SwingUtilities.invokeLater(DownloadMapsWindow::showDownloadMapsWindow);
  }

  @FXML
  @VisibleForTesting
  void showSettingsMenu() {
    screenController.switchScreen(FxmlManager.SETTINGS_PANE);
  }

  @FXML
  @VisibleForTesting
  void showAboutSection() {
    screenController.switchScreen(FxmlManager.ABOUT_INFORMATION);
  }

  @FXML
  @VisibleForTesting
  void showExitConfirmDialog() {
    ((RootActionPane) mainOptions.getScene().getWindow().getUserData()).promptExit();
  }

  @Override
  public void connect(final ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return mainOptions;
  }
}
