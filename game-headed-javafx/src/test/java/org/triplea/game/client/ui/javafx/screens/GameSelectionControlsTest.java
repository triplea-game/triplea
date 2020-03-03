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
    final BorderPane rootNode = mock(BorderPane.class);
    final GameSelectionControls aboutInformation = new GameSelectionControls(rootNode);
    assertEquals(rootNode, aboutInformation.getNode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testBackButton() {
    final ScreenController<FxmlManager> screenControllerMock = mock(ScreenController.class);
    final GameSelectionControls aboutInformation = new GameSelectionControls();
    aboutInformation.connect(screenControllerMock);

    aboutInformation.back();
    verify(screenControllerMock).switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLocalGameButton() {
    final ScreenController<FxmlManager> screenControllerMock = mock(ScreenController.class);
    final GameSelectionControls aboutInformation = new GameSelectionControls();
    aboutInformation.connect(screenControllerMock);

    aboutInformation.showLocalGameMenu();
    verify(screenControllerMock).switchScreen(FxmlManager.MAP_SELECTION);
  }
}
