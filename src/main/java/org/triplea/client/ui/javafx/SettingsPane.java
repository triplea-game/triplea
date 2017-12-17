package org.triplea.client.ui.javafx;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import games.strategy.triplea.settings.SettingType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

class SettingsPane extends StackPane {
  private final TripleA triplea;

  @FXML
  private TabPane tabPane;


  /**
   * @param triplea The root pane.
   * @throws IOException If the FXML file is not present.
   */
  public SettingsPane(final TripleA triplea) throws IOException {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.SETTINGS_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.triplea = triplea;
    Arrays.stream(SettingType.values()).forEach(type -> {
      final Tab tab = new Tab(loader.getResources().getString("settings.tab." + type.toString().toLowerCase()));
      final GridPane pane = new GridPane();
      pane.setPadding(new Insets(5, 0, 0, 0));
      pane.setVgap(5);
      tab.setContent(new ScrollPane(pane));
      Arrays.stream(ClientSettingJavaFxUiBinding.values())
          .filter(b -> b.getCategory() == type)
          .forEach(b -> {
            final Label description = new Label();
            final Node node = b.buildSelectionComponent();
            description.setText(loader.getResources().getString(
                getSettingLocalizationKey(node, b.name().toLowerCase())));
            pane.addColumn(0, description);
            pane.addColumn(1, node);
          });
      if (!pane.getChildren().isEmpty()) {
        tabPane.getTabs().add(tab);
      }
    });
  }

  @FXML
  private void back() {
    // TODO check if some changes haven't been saved
    triplea.returnToMainMenu(this);
  }

  @FXML
  private void reset() {
    Arrays.stream(ClientSettingJavaFxUiBinding.values()).forEach(ClientSettingJavaFxUiBinding::reset);
  }

  @FXML
  private void resetToDefault() {
    Arrays.stream(ClientSettingJavaFxUiBinding.values()).forEach(ClientSettingJavaFxUiBinding::resetToDefault);
  }

  @FXML
  private void save() {
    Arrays.stream(ClientSettingJavaFxUiBinding.values())
        .map(ClientSettingJavaFxUiBinding::readValues)
        .map(Map::entrySet)
        .flatMap(Collection::stream)
        .forEach(entry -> entry.getKey().save(entry.getValue()));
    // TODO visual feedback
  }


  private static String getSettingLocalizationKey(final Node rootNode, final String name) {
    return "settings." + rootNode.getClass().getSimpleName().toLowerCase() + "." + name;
  }
}
