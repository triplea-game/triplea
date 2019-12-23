package org.triplea.game.client.ui.javafx.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javafx.scene.layout.BorderPane;
import org.junit.jupiter.api.Test;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

class GameSelectionControlsTest {

  @Test
  void testGetNode() {
    final BorderPane mock = mock(BorderPane.class);
    final GameSelectionControls aboutInformation = new GameSelectionControls(mock);
    assertEquals(mock, aboutInformation.getNode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testBackButton() {
    final ScreenController<FxmlManager> mock = mock(ScreenController.class);
    final GameSelectionControls aboutInformation = new GameSelectionControls();
    aboutInformation.connect(mock);

    aboutInformation.back();
    verify(mock).switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLocalGameButton() {
    final ScreenController<FxmlManager> mock = mock(ScreenController.class);
    final GameSelectionControls aboutInformation = new GameSelectionControls();
    aboutInformation.connect(mock);

    aboutInformation.showLocalGameMenu();
    verify(mock).switchScreen(FxmlManager.MAP_SELECTION);
  }
}
