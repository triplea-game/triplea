package org.triplea.game.client.ui.javafx.util;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import javafx.fxml.FXMLLoader;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Enum used to build Node instances from FXML files. */
public enum FxmlManager {
  ROOT_CONTAINER("/org/triplea/game/client/ui/javafx/fxml/TripleAMain.fxml"),

  MAIN_MENU_PANE("/org/triplea/game/client/ui/javafx/fxml/TripleAMainMenu.fxml"),

  SETTINGS_PANE("/org/triplea/game/client/ui/javafx/fxml/TripleASettings.fxml"),

  ABOUT_INFORMATION("/org/triplea/game/client/ui/javafx/fxml/AboutInformation.fxml"),

  GAME_SELECTION_CONTROLS("/org/triplea/game/client/ui/javafx/fxml/GameSelectionControls.fxml"),

  MAIN_MENU_CONTROLS("/org/triplea/game/client/ui/javafx/fxml/MainMenuControls.fxml"),

  MAP_SELECTION("/org/triplea/game/client/ui/javafx/fxml/MapSelection.fxml");

  private static final String LANG_CLASS_BASENAME =
      "org.triplea.game.client.ui.javafx.lang.TripleA";

  private final String value;

  FxmlManager(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /**
   * Helper class to wrap a controller and the root node in a single class.
   *
   * @param <C> Type of the controller.
   * @param <N> Type of the root node.
   */
  @AllArgsConstructor
  @Getter
  public static final class LoadedNode<C, N> {
    private final C controller;
    private final N node;
  }

  /**
   * Simplified way to load a Node & Controller.
   *
   * @return A LoadedNode object
   */
  public <C, N> LoadedNode<C, N> load() {
    try {
      final FXMLLoader loader = new FXMLLoader(getClass().getResource(value));
      // TODO load locale based on user setting
      loader.setResources(ResourceBundle.getBundle(LANG_CLASS_BASENAME, new Locale("en", "US")));
      loader.load();
      return new LoadedNode<>(loader.getController(), loader.getRoot());
    } catch (final IOException e) {
      throw new IllegalStateException("Failed to load FXML " + value, e);
    }
  }
}
