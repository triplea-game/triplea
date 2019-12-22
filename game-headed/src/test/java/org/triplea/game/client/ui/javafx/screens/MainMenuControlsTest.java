package org.triplea.game.client.ui.javafx.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.support.ReflectionSupport;
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

    @SuppressWarnings("unchecked")
    private void injectScene(final Scene scene) throws Exception {
      final Class<?> clazz =
          ReflectionSupport.findNestedClasses(
                  Node.class, ReadOnlyObjectWrapper.class::isAssignableFrom)
              .get(0);
      final Constructor<?> constructor = clazz.getDeclaredConstructor(Node.class);
      constructor.setAccessible(true);
      final ObjectPropertyBase<Scene> propertyBase =
          (ObjectPropertyBase<Scene>) constructor.newInstance(mock);
      final Field sceneField = Node.class.getDeclaredField("scene");
      sceneField.setAccessible(true);
      sceneField.set(mock, propertyBase);
      propertyBase.set(scene);
    }

    private void injectWindow(final Scene scene, final Window window) throws Exception {
      final Field windowField = Scene.class.getDeclaredField("window");
      windowField.setAccessible(true);
      windowField.set(scene, new ReadOnlyObjectWrapper<>(window));
    }

    @Test
    void testGetNode() {
      assertEquals(mock, aboutInformation.getNode());
    }

    @Test
    void testShowExit() throws Exception {
      final Scene scene = mock(Scene.class);
      injectScene(scene);
      final Window window = mock(Window.class);
      injectWindow(scene, window);
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
