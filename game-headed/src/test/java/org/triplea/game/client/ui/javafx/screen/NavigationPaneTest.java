package org.triplea.game.client.ui.javafx.screen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

class NavigationPaneTest {
  private final Pane mock = mock(Pane.class);
  private final NavigationPane navigationPane = new NavigationPane(mock);


  @Test
  void testGetNode() {
    assertEquals(mock, navigationPane.getNode());
  }

  @Test
  void testSwapInvalidScreen() {
    final Exception exception = assertThrows(IllegalArgumentException.class, () -> navigationPane.switchScreen(
        FxmlManager.MAIN_MENU_CONTROLS));
    assertThat(exception.getMessage()).contains(FxmlManager.MAIN_MENU_CONTROLS.toString());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSwapValidScreen() {
    final ControlledScreen<NavigationPane> mock = mock(ControlledScreen.class);
    final Node node = mock(Node.class);
    final Node node2 = mock(Node.class);
    when(mock.getNode()).thenReturn(node, node2);
    final ObservableList<Node> children = FXCollections.observableList(new ArrayList<>());
    when(this.mock.getChildren()).thenReturn(children);

    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS, mock);

    verify(mock).getNode();

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS, mock);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    assertEquals(1, children.size());
    assertEquals(node2, children.get(0));
  }

  @Test
  void testParent() {
    final NavigationPane mock2 = mock(NavigationPane.class);
    navigationPane.setParent(mock2);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    verify(mock2).switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
