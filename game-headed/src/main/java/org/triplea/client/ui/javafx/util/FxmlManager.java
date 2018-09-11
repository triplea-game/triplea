package org.triplea.client.ui.javafx.util;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;

/**
 * Enum with relative Paths to fxml-related resources.
 */
public enum FxmlManager {
  ROOT_CONTAINER("/org/triplea/client/ui/javafx/fxml/TripleAMain.fxml"),

  DOWNLOAD_PANE("/org/triplea/client/ui/javafx/fxml/TripleADownload.fxml"),

  MAIN_MENU_PANE("/org/triplea/client/ui/javafx/fxml/TripleAMainMenu.fxml"),

  SETTINGS_PANE("/org/triplea/client/ui/javafx/fxml/TripleASettings.fxml"),

  LANG_CLASS_BASENAME("org.triplea.client.ui.javafx.lang.TripleA"),

  STYLESHEET_MAIN("/org/triplea/client/ui/javafx/css/main.css"),

  FONT_PATH("/org/triplea/client/ui/javafx/css/fonts/1942-report.ttf"),

  ICON_LOCATION("/games/strategy/engine/framework/ta_icon.png");

  private final String value;

  FxmlManager(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /**
   * Simplified way of getting an {@link FXMLLoader} with the default settings for TripleA.
   *
   * @param location The FXML File to load
   * @return An FXMLLoader object
   */
  public static FXMLLoader getLoader(final URL location) {
    final FXMLLoader loader = new FXMLLoader(location);
    // TODO load locale based on user setting
    loader.setResources(ResourceBundle.getBundle(LANG_CLASS_BASENAME.toString(), new Locale("en", "US")));
    return loader;
  }
}
