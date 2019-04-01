package org.triplea.game.client.ui.javafx;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.util.ClientSettingJavaFxUiBinding;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.triplea.settings.GameSetting;
import games.strategy.triplea.settings.SelectionComponent;
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
  private final NavigationPane navigationPane;
  private final Map<ClientSettingJavaFxUiBinding, SelectionComponent<Region>> selectionComponentsBySetting =
      Arrays.stream(ClientSettingJavaFxUiBinding.values()).collect(Collectors.toMap(
          Function.identity(),
          ClientSettingJavaFxUiBinding::newSelectionComponent,
          (oldValue, newValue) -> {
            throw new AssertionError("impossible condition: enum contains duplicate values");
          },
          () -> new EnumMap<>(ClientSettingJavaFxUiBinding.class)));

  @FXML
  private TabPane tabPane;

  /**
   * Initializes a new instance of the SettingsPane class.
   *
   * @param navigationPane The root pane.
   * @throws IOException If the FXML file is not present.
   */
  SettingsPane(final NavigationPane navigationPane) throws IOException {
    final FXMLLoader loader = FxmlManager.getLoader(getClass().getResource(FxmlManager.SETTINGS_PANE.toString()));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
    this.navigationPane = navigationPane;
    final ResourceBundle bundle = loader.getResources();
    Arrays.stream(SettingType.values()).forEach(type -> {
      final Tab tab = new Tab();
      final GridPane pane = new GridPane();
      pane.setPadding(new Insets(15, 10, 5, 10));
      pane.setVgap(5);
      tab.setContent(new ScrollPane(pane));
      pane.prefWidthProperty().bind(tabPane.widthProperty());
      selectionComponentsBySetting.entrySet().stream()
          .filter(entry -> entry.getKey().getType().equals(type))
          .forEach(entry -> {
            final Tooltip tooltip =
                new Tooltip(bundle.getString("settings.tooltip." + entry.getKey().name().toLowerCase()));
            final Region element = entry.getValue().getUiComponent();
            final Label description = new Label(bundle.getString(getSettingLocalizationKey(element, entry.getKey())));
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
    navigationPane.switchScreen(MainMenuPane.class);
  }

  @FXML
  private void reset() {
    selectionComponentsBySetting.values().forEach(SelectionComponent::reset);
  }

  @FXML
  private void resetToDefault() {
    selectionComponentsBySetting.values().forEach(SelectionComponent::resetToDefault);
  }

  @FXML
  private void save() {
    final SelectionComponent.SaveContext context = new SelectionComponent.SaveContext() {
      @Override
      public void reportError(
          final GameSetting<?> gameSetting,
          final String message,
          final @Nullable Object value,
          final SelectionComponent.SaveContext.ValueSensitivity valueSensitivity) {}

      @Override
      public <T> void setValue(
          final GameSetting<T> gameSetting,
          final @Nullable T value,
          final SelectionComponent.SaveContext.ValueSensitivity valueSensitivity) {
        gameSetting.setValue(value);
      }
    };
    selectionComponentsBySetting.values().forEach(selectionComponent -> selectionComponent.save(context));
    // TODO visual feedback
  }

  private static String getSettingLocalizationKey(final Node rootNode, final Enum<?> name) {
    return "settings." + rootNode.getClass().getSimpleName().toLowerCase() + "." + name.name().toLowerCase();
  }
}
