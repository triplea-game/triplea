package org.triplea.game.client.ui.javafx.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

class MainMenuControlsTest {

  @Nested
  class RootNodeTest {
    private final VBox mock = mock(VBox.class);
    private final MainMenuControls aboutInformation = new MainMenuControls(mock);

    @Test
    void testGetNode() {
      assertEquals(mock, aboutInformation.getNode());
    }

    @Test
    void testShowExit() {
      final Scene scene = mock(Scene.class);
      when(mock.getScene()).thenReturn(scene);
      final Window window = mock(Window.class);
      when(scene.getWindow()).thenReturn(window);
      final RootActionPane rootActionPane = mock(RootActionPane.class);
      when(window.getUserData()).thenReturn(rootActionPane);

      aboutInformation.showExitConfirmDialog();

      verify(rootActionPane).promptExit();
    }

    @Test
    void testSetupAnimation() {
      final Set<Node> nodes = spy(new HashSet<>());
      when(mock.lookupAll(".button")).thenReturn(nodes);

      aboutInformation.initialize();

      verify(nodes).forEach(any());
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  class ScreenSwitchingTest {

    @Mock private ScreenController<FxmlManager> mock;
    private final MainMenuControls aboutInformation = new MainMenuControls();

    @BeforeEach
    void setup() {
      aboutInformation.connect(mock);
    }

    @Test
    void testShowPlayOptions() {
      aboutInformation.showPlayOptions();
      verify(mock).switchScreen(FxmlManager.GAME_SELECTION_CONTROLS);
    }

    @Test
    void testShowSettingsMenu() {
      aboutInformation.showSettingsMenu();
      verify(mock).switchScreen(FxmlManager.SETTINGS_PANE);
    }

    @Test
    void testShowAboutSection() {
      aboutInformation.showAboutSection();
      verify(mock).switchScreen(FxmlManager.ABOUT_INFORMATION);
    }
  }
}
