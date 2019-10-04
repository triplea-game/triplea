package org.triplea.game.client.ui.javafx.screens;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.settings.GameSetting;
import games.strategy.triplea.settings.SelectionComponent;
import games.strategy.triplea.settings.SettingType;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.fxml.FXML;
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
import javax.annotation.Nullable;
import lombok.NoArgsConstructor;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.ClientSettingJavaFxUiBinding;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

/**
 * SettingsPane Controller class that represents the JavaFX implementation of our Settings
 * framework.
 */
@NoArgsConstructor
public class SettingsPane implements ControlledScreen<ScreenController<FxmlManager>> {
  private ScreenController<FxmlManager> screenController;
  private Map<ClientSettingJavaFxUiBinding, SelectionComponent<Region>>
      selectionComponentsBySetting;

  @FXML private TabPane tabPane;

  @FXML private StackPane root;

  @FXML private ResourceBundle resources;

  @VisibleForTesting
  SettingsPane(final StackPane root) {
    this.root = root;
  }

  @SuppressWarnings("unused")
  @FXML
  private void initialize() {
    selectionComponentsBySetting =
        Arrays.stream(ClientSettingJavaFxUiBinding.values())
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    ClientSettingJavaFxUiBinding::newSelectionComponent,
                    (oldValue, newValue) -> {
                      throw new AssertionError(
                          "impossible condition: enum contains duplicate values");
                    },
                    () -> new EnumMap<>(ClientSettingJavaFxUiBinding.class)));

    Arrays.stream(SettingType.values())
        .forEach(
            type -> {
              final Tab tab = new Tab();
              final GridPane pane = new GridPane();
              pane.setPadding(new Insets(15, 10, 5, 10));
              pane.setVgap(5);
              tab.setContent(new ScrollPane(pane));
              pane.prefWidthProperty().bind(tabPane.widthProperty());
              selectionComponentsBySetting.entrySet().stream()
                  .filter(entry -> entry.getKey().getType().equals(type))
                  .forEach(
                      entry -> {
                        final Tooltip tooltip =
                            new Tooltip(
                                resources.getString(
                                    "settings.tooltip." + entry.getKey().name().toLowerCase()));
                        final Region element = entry.getValue().getUiComponent();
                        final Label description =
                            new Label(
                                resources.getString(
                                    getSettingLocalizationKey(element, entry.getKey())));
                        description.setTooltip(tooltip);
                        addTooltipRecursively(element, tooltip);
                        pane.addColumn(0, description);
                        pane.addColumn(1, element);
                      });
              if (!pane.getChildren().isEmpty()) {
                tab.setText(resources.getString("settings.tab." + type.toString().toLowerCase()));
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
  @VisibleForTesting
  void back() {
    screenController.switchScreen(FxmlManager.MAIN_MENU_PANE);
  }

  @SuppressWarnings("unused")
  @FXML
  private void reset() {
    selectionComponentsBySetting.values().forEach(SelectionComponent::reset);
  }

  @SuppressWarnings("unused")
  @FXML
  private void resetToDefault() {
    selectionComponentsBySetting.values().forEach(SelectionComponent::resetToDefault);
  }

  @SuppressWarnings("unused")
  @FXML
  private void save() {
    final SelectionComponent.SaveContext context =
        new SelectionComponent.SaveContext() {
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
    selectionComponentsBySetting
        .values()
        .forEach(selectionComponent -> selectionComponent.save(context));
    // TODO visual feedback
  }

  private static String getSettingLocalizationKey(final Node rootNode, final Enum<?> name) {
    return "settings."
        + rootNode.getClass().getSimpleName().toLowerCase()
        + "."
        + name.name().toLowerCase();
  }

  @Override
  public void connect(final ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return root;
  }
}
