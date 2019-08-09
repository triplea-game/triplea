package org.triplea.game.client.ui.javafx.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

class SettingsPaneTest {

  @Test
  void testGetNode() {
    final StackPane mock = mock(StackPane.class);
    final SettingsPane aboutInformation = new SettingsPane(mock);
    assertEquals(mock, aboutInformation.getNode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testBackButton() {
    final ScreenController<FxmlManager> mock = mock(ScreenController.class);
    final SettingsPane aboutInformation = new SettingsPane();
    aboutInformation.connect(mock);

    aboutInformation.back();
    verify(mock).switchScreen(FxmlManager.MAIN_MENU_PANE);
  }
}
