package org.triplea.game.client.ui.javafx.screens;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.properties.NumberProperty;
import games.strategy.engine.framework.startup.launcher.LocalLauncher;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.Constants;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import lombok.extern.java.Log;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

@Log
public class RoleSelection implements ControlledScreen<ScreenController<FxmlManager>> {

  static final String SELECTED_MAP_KEY = "selectedMap";
  private static final String DISABLE_TEXT = "Disable";

  private final Map<GamePlayer, ComboBox<String>> roleForPlayers = new HashMap<>();
  private final Map<GamePlayer, Spinner<Integer>> incomeForPlayers = new HashMap<>();
  private final Map<GamePlayer, Spinner<Integer>> pusForPlayers = new HashMap<>();

  @FXML private ComboBox<String> allSelectorCheckbox;

  @FXML private GridPane factionGrid;

  @FXML private VBox root;

  @FXML private CheckBox resourceModifierCheckbox;

  private ScreenController<FxmlManager> screenController;
  private GameData gameData;

  @Override
  public void connect(final ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return root;
  }

  @FXML
  void initialize() {
    allSelectorCheckbox.getItems().setAll(PlayerType.playerTypes());
  }

  @Override
  public void onShow(final Map<String, Object> data) {
    roleForPlayers.clear();
    factionGrid
        .getChildren()
        .removeIf(
            node -> {
              final Integer row = GridPane.getRowIndex(node);
              return row != null && row > 0;
            });
    gameData =
        (GameData)
            Optional.ofNullable(data.get(SELECTED_MAP_KEY))
                .orElseThrow(
                    () -> new IllegalStateException("Missing GameData when calling screen"));
    final List<String> availablePlayers = List.of(PlayerType.playerTypes());
    for (final GamePlayer playerId : gameData.getPlayerList()) {
      final var name = new Label(playerId.getName());
      final ComboBox<String> controllingPlayer =
          new ComboBox<>(FXCollections.observableArrayList(availablePlayers));
      controllingPlayer.getSelectionModel().select(0);
      if (playerId.getCanBeDisabled()) {
        controllingPlayer.getItems().add(DISABLE_TEXT);
      }
      roleForPlayers.put(playerId, controllingPlayer);
      final Collection<String> playerAlliances =
          gameData.getAllianceTracker().getAlliancesPlayerIsIn(playerId);
      final var faction = new Button(playerAlliances.toString());
      faction.setOnAction(
          e -> {
            final int targetIndex = controllingPlayer.getSelectionModel().getSelectedIndex();
            playerAlliances.stream()
                .map(gameData.getAllianceTracker()::getPlayersInAlliance)
                .flatMap(Collection::stream)
                .map(roleForPlayers::get)
                .filter(stringComboBox -> stringComboBox.getItems().size() > targetIndex)
                .map(ComboBox::getSelectionModel)
                .forEach(selectionModel -> selectionModel.select(targetIndex));
          });
      final var income = new Spinner<Integer>(0, 100, 100);
      income.setDisable(true);
      incomeForPlayers.put(playerId, income);
      final var pus = new Spinner<Integer>(0, 100, 0);
      pus.setDisable(true);
      pusForPlayers.put(playerId, pus);
      factionGrid.addRow(factionGrid.getRowCount(), name, controllingPlayer, faction, income, pus);
    }
  }

  @Override
  public Map<String, Class<?>> getValidTypes() {
    return Map.of(SELECTED_MAP_KEY, GameData.class);
  }

  @FXML
  void setAllTo() {
    factionGrid.getChildren().stream()
        .filter(ComboBox.class::isInstance)
        .map(node -> (ComboBox<?>) node)
        .forEach(
            comboBox ->
                comboBox
                    .getSelectionModel()
                    .select(allSelectorCheckbox.getSelectionModel().getSelectedIndex()));
  }

  @FXML
  void cancelMapSelection() {
    screenController.switchScreen(FxmlManager.MAP_SELECTION);
  }

  @FXML
  void startGame() {
    if (resourceModifierCheckbox.isSelected()) {
      incomeForPlayers.forEach(
          (gamePlayer, spinner) ->
              ((NumberProperty)
                      gameData
                          .getProperties()
                          .getPlayerProperty(Constants.getIncomePercentageFor(gamePlayer)))
                  .setValue(spinner.getValue()));
      pusForPlayers.forEach(
          (gamePlayer, spinner) ->
              ((NumberProperty)
                      gameData
                          .getProperties()
                          .getPlayerProperty(Constants.getPuIncomeBonus(gamePlayer)))
                  .setValue(spinner.getValue()));
    }
    final List<Entry<String, String>> flatMapping =
        roleForPlayers.entrySet().stream()
            .map(
                entry ->
                    Map.entry(
                        entry.getKey().getName(),
                        entry.getValue().getSelectionModel().getSelectedItem()))
            .collect(Collectors.toUnmodifiableList());
    LocalLauncher.create(flatMapping, DISABLE_TEXT::equals, gameData).launch();
    cancelMapSelection();
  }

  @FXML
  void toggleResourceModifiers() {
    factionGrid.getChildren().stream()
        .filter(
            node -> {
              final Integer column = GridPane.getColumnIndex(node);
              final Integer row = GridPane.getRowIndex(node);
              return row != null && column != null && column >= 3 && row > 0;
            })
        .forEach(node -> node.setDisable(!resourceModifierCheckbox.isSelected()));
  }
}
