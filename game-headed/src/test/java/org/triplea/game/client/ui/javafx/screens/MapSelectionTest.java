package org.triplea.game.client.ui.javafx.screens;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.engine.framework.ui.GameChooserEntry;
import java.lang.reflect.Field;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.game.client.parser.GameDetector;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

public class MapSelectionTest {
  private final String mapName = "\n Test Name \n";
  private final String mapNotes = "  \n  Test Notes  \r\n  ";

  private GameDetector gameDetector;
  private MapSelection mapSelection;

  @BeforeEach
  void setUp() {
    gameDetector = mock(GameDetector.class);
    mapSelection = new MapSelection(gameDetector, String::concat);
  }

  @Test
  void onShowTriggersLoadingCorrectly() {
    mapSelection.initialize();

    verify(gameDetector).discoverGames(any());

    clearInvocations(gameDetector);
  }

  @Test
  void mapSelectionObjectReturnsCorrectNode() {
    final Node node = mock(Node.class);
    mapSelection.setRoot(node);

    assertThat(mapSelection.getNode(), is(equalTo(node)));
  }

  @SuppressWarnings("unchecked")
  @Test
  void backKeyWorks() {
    final ScreenController<FxmlManager> manager = mock(ScreenController.class);

    mapSelection.connect(manager);

    mapSelection.backToGameSelection();

    verify(manager).switchScreen(FxmlManager.GAME_SELECTION_CONTROLS);
  }

  @Test
  void closeButtonClosesDetails() {
    final Node node = mock(Pane.class);
    mapSelection.setPreviewContainer(node);

    mapSelection.closeDetails();

    verify(node).setVisible(false);
  }

  @Test
  void detailsButtonShowsDetails() throws Exception {
    final Node previewContainer = mock(Node.class);
    mapSelection.setPreviewContainer(previewContainer);

    final WebView previewWindow = mock(WebView.class);
    final var webEngine = mockWebEngine();
    when(previewWindow.getEngine()).thenReturn(webEngine);
    mapSelection.setPreviewWindow(previewWindow);

    final GameChooserEntry gameChooserEntry = mockGameChooserEntryWithNotes();
    mapSelection.setSelectedGame(gameChooserEntry);

    mapSelection.showDetails();

    verify(previewContainer).setVisible(true);
    verify(webEngine).loadContent(mapNotes.trim() + mapName);
  }

  /**
   * {@link WebEngine} has a static initializer that expects a running JavaFX environment. To avoid
   * exceptions we manually set a private field to skip validation when mocking.
   */
  private WebEngine mockWebEngine() throws Exception {
    final Class<?> clazz = Class.forName("com.sun.glass.ui.Screen");
    final Field screensField = clazz.getDeclaredField("screens");
    screensField.setAccessible(true);
    screensField.set(null, List.of());
    return mock(WebEngine.class);
  }

  private GameChooserEntry mockGameChooserEntryWithNotes() {
    final GameChooserEntry gameChooserEntry = mock(GameChooserEntry.class);
    final GameData gameData = mock(GameData.class);
    final GameProperties gameProperties = mock(GameProperties.class);
    when(gameProperties.get("mapName", "")).thenReturn(mapName);
    when(gameProperties.get("notes", "")).thenReturn(mapNotes);
    when(gameData.getProperties()).thenReturn(gameProperties);
    when(gameChooserEntry.getGameData()).thenReturn(gameData);
    return gameChooserEntry;
  }
}
