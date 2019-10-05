package org.triplea.game.client.ui.javafx.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

class AboutInformationTest {

  @Test
  void testGetNode() {
    final VBox mock = mock(VBox.class);
    final AboutInformation aboutInformation = new AboutInformation(mock);
    assertEquals(mock, aboutInformation.getNode());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testBackButton() {
    final ScreenController<FxmlManager> mock = mock(ScreenController.class);
    final AboutInformation aboutInformation = new AboutInformation();
    aboutInformation.connect(mock);

    aboutInformation.back();
    verify(mock).switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
