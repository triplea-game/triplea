package org.triplea.game.client.ui.javafx.screen;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

class NavigationPaneTest {
  private final Pane mockedPane = mock(Pane.class);
  private final NavigationPane navigationPane = new NavigationPane(mockedPane);

  @Test
  void testGetNode() {
    assertEquals(mockedPane, navigationPane.getNode());
  }

  @Test
  void testSwapInvalidScreen() {
    final Exception exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS));
    assertThat(exception.getMessage(), containsString(FxmlManager.MAIN_MENU_CONTROLS.toString()));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSwapValidScreen() {
    final ControlledScreen<NavigationPane> mock = mock(ControlledScreen.class);
    final Node node = mock(Node.class);
    final Node node2 = mock(Node.class);
    when(mock.getNode()).thenReturn(node, node2);
    final ObservableList<Node> children = FXCollections.observableArrayList();
    when(mockedPane.getChildren()).thenReturn(children);

    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS, mock);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS, mock);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    assertEquals(1, children.size());
    assertEquals(node2, children.get(0));
  }

  @Test
  void verifyEarlyExceptionOccursWhenUsingInvalidTypeForScreenChange() {
    final ControlledScreen<NavigationPane> mock = givenControlledScreenWithTestStringParam();

    // Mismatching class
    assertThrows(
        IllegalArgumentException.class,
        () ->
            navigationPane.switchScreen(
                FxmlManager.MAIN_MENU_CONTROLS, Map.of("Test1", new Object())));
    // Missing declaration
    assertThrows(
        IllegalArgumentException.class,
        () ->
            navigationPane.switchScreen(
                FxmlManager.MAIN_MENU_CONTROLS, Map.of("Test2", new Object())));

    verify(mock, never()).onShow(any());
  }

  @Test
  void verifyOnShowGetsCalledWithCorrectObject() {
    final ControlledScreen<NavigationPane> mock = givenControlledScreenWithTestStringParam();

    final Map<String, Object> values = Map.of("Test", "Value!");

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS, values);

    verify(mock).onShow(values);
  }

  @Test
  void verifyOnShowGetsCalledWithEmptyMap() {
    final ControlledScreen<NavigationPane> mock = givenControlledScreenWithTestStringParam();

    final Map<String, Object> values = Map.of();

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS, values);

    verify(mock).onShow(values);
  }

  @SuppressWarnings("unchecked")
  private ControlledScreen<NavigationPane> givenControlledScreenWithTestStringParam() {
    final ControlledScreen<NavigationPane> mock = mock(ControlledScreen.class);
    when(mock.getNode()).thenReturn(mock(Node.class));
    when(mock.getValidTypes()).thenReturn(Map.of("Test", String.class));
    when(mockedPane.getChildren()).thenReturn(FXCollections.observableArrayList());

    navigationPane.registerScreen(FxmlManager.MAIN_MENU_CONTROLS, mock);
    return mock;
  }

  @Test
  void testParent() {
    final NavigationPane mock2 = mock(NavigationPane.class);
    navigationPane.setParent(mock2);

    navigationPane.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    verify(mock2).switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
