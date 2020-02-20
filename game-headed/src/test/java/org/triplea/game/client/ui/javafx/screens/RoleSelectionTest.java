package org.triplea.game.client.ui.javafx.screens;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.triplea.game.client.ui.javafx.screens.RoleSelection.SELECTED_MAP_KEY;

import games.strategy.engine.data.GameData;
import games.strategy.engine.framework.startup.ui.PlayerType;
import java.util.List;
import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.game.client.ui.javafx.UserAgentStylesheetTest;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

public class RoleSelectionTest extends UserAgentStylesheetTest {

  private final RoleSelection roleSelection = new RoleSelection();

  @Test
  void correctRootNodeIsReturned() {
    final VBox root = mock(VBox.class);
    roleSelection.setRoot(root);

    assertThat(roleSelection.getNode(), is(root));
  }

  @SuppressWarnings("unchecked")
  @Test
  void allSelectorCheckboxIsInitializedWithCorrectValues() {
    final ObservableList<String> observableList = FXCollections.observableArrayList();
    final ComboBox<String> comboBox = mock(ComboBox.class);
    when(comboBox.getItems()).thenReturn(observableList);
    roleSelection.setAllSelectorCheckbox(comboBox);

    roleSelection.initialize();

    assertThat(observableList, is(List.of(PlayerType.playerTypes())));
  }

  // TODO Add Test for onShow method

  @Test
  void keyTypesAreCorrect() {
    assertThat(roleSelection.getValidTypes(), is(Map.of(SELECTED_MAP_KEY, GameData.class)));
  }

  @Test
  void globalPlayerTypeSelectionWorks() {
    final List<ComboBox<String>> comboBoxes = mockComboBoxes();
    createSelectionModelMock(comboBoxes.get(0), 1337);
    final var selectionModel1 = createSelectionModelMock(comboBoxes.get(1), 0);
    final var selectionModel2 = createSelectionModelMock(comboBoxes.get(2), 0);

    roleSelection.setAllTo();

    verify(selectionModel1).select(1337);
    verify(selectionModel2).select(1337);
  }

  @SuppressWarnings("unchecked")
  private List<ComboBox<String>> mockComboBoxes() {
    final ComboBox<String> globalBox = mock(ComboBox.class);
    final ComboBox<String> comboBox1 = mock(ComboBox.class);
    final ComboBox<String> comboBox2 = mock(ComboBox.class);

    final GridPane gridPane = mock(GridPane.class);
    when(gridPane.getChildren())
        .thenReturn(FXCollections.observableArrayList(mock(Node.class), comboBox1, comboBox2));

    roleSelection.setFactionGrid(gridPane);
    roleSelection.setAllSelectorCheckbox(globalBox);
    return List.of(globalBox, comboBox1, comboBox2);
  }

  @SuppressWarnings("unchecked")
  private SingleSelectionModel<String> createSelectionModelMock(
      final ComboBox<String> comboBox, final int index) {
    final SingleSelectionModel<String> mock = mock(SingleSelectionModel.class);
    when(comboBox.getSelectionModel()).thenReturn(mock);
    when(mock.getSelectedIndex()).thenReturn(index);
    return mock;
  }

  @SuppressWarnings("unchecked")
  @Test
  void canCancelMapSelection() {
    final ScreenController<FxmlManager> controller = mock(ScreenController.class);
    roleSelection.connect(controller);

    roleSelection.cancelMapSelection();

    verify(controller).switchScreen(FxmlManager.MAP_SELECTION);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void canToggleResourceModifiers(final boolean checked) {
    final CheckBox checkBox = mock(CheckBox.class);
    when(checkBox.isSelected()).thenReturn(checked);

    final Node node1 = mock(Node.class);
    final Node node2 = mockNodeInGridPane(0, 2);
    final Node node3 = mockNodeInGridPane(0, 3);
    final Node node4 = mockNodeInGridPane(1, 2);
    final Node node5 = mockNodeInGridPane(1, 3);

    final GridPane factionGrid = mock(GridPane.class);
    when(factionGrid.getChildren())
        .thenReturn(FXCollections.observableArrayList(node1, node2, node3, node4, node5));
    roleSelection.setFactionGrid(factionGrid);
    roleSelection.setResourceModifierCheckbox(checkBox);

    roleSelection.toggleResourceModifiers();

    verify(node1, never()).setDisable(anyBoolean());
    verify(node2, never()).setDisable(anyBoolean());
    verify(node3, never()).setDisable(anyBoolean());
    verify(node4, never()).setDisable(anyBoolean());
    verify(node5).setDisable(!checked);
  }

  private Node mockNodeInGridPane(final int row, final int col) {
    final Node node = mock(Node.class);
    when(node.hasProperties()).thenReturn(true);
    when(node.getProperties())
        .thenReturn(
            FXCollections.observableMap(
                Map.of(
                    "gridpane-row", row,
                    "gridpane-column", col)));
    return node;
  }
}
