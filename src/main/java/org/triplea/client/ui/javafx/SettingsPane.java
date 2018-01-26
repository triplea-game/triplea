package org.triplea.client.ui.javafx;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.ResourceBundle;

import org.triplea.client.ui.javafx.util.ClientSettingJavaFxUiBinding;
import org.triplea.client.ui.javafx.util.FxmlManager;

import games.strategy.triplea.settings.SettingType;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
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
    final ResourceBundle bundle = loader.getResources();
    Arrays.stream(SettingType.values()).forEach(type -> {
      final Tab tab = new Tab();
      final GridPane pane = new GridPane();
      pane.setPadding(new Insets(15, 10, 5, 10));
      pane.setVgap(5);
      tab.setContent(new ScrollPane(pane));
      pane.prefWidthProperty().bind(tabPane.widthProperty());
      Arrays.stream(ClientSettingJavaFxUiBinding.values())
          .filter(b -> b.getCategory() == type)
          .forEach(b -> {
            final Tooltip tooltip = new Tooltip(bundle.getString("settings.tooltip." + b.name().toLowerCase()));
            final Region element = b.buildSelectionComponent();
            final Label description = new Label(bundle.getString(getSettingLocalizationKey(element, b)));
            description.setTooltip(tooltip);
            addTooltipRecursively(element, tooltip);
            pane.addColumn(0, description);
            pane.addColumn(1, element);
          });
      if (!pane.getChildren().isEmpty()) {
        tab.setText(bundle.getString("settings.tab." + type.toString().toLowerCase()));
        final ColumnConstraints constraint0 = new ColumnConstraints();
        final ColumnConstraints constraint1 = new ColumnConstraints();
        constraint0.setHgrow(Priority.ALWAYS);
        constraint1.setHgrow(Priority.NEVER);
        constraint1.setHalignment(HPos.RIGHT);
        pane.getColumnConstraints().addAll(constraint0, constraint1);
        tabPane.getTabs().add(tab);
      }
    });
  }

  private static void addTooltipRecursively(final Node node, final Tooltip tooltip) {
    if (node instanceof Control) {
      ((Control) node).setTooltip(tooltip);
    } else if (node instanceof Parent) {
      ((Parent) node).getChildrenUnmodifiable().forEach(n -> addTooltipRecursively(n, tooltip));
    }
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


  private static String getSettingLocalizationKey(final Node rootNode, final Enum<?> name) {
    return "settings." + rootNode.getClass().getSimpleName().toLowerCase() + "." + name.name().toLowerCase();
  }
}
