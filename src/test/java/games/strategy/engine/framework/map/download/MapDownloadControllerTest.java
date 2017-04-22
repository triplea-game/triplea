package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.MapDownloadController.TutorialMapPreferences;
import games.strategy.engine.framework.map.download.MapDownloadController.UserMaps;

@RunWith(MockitoJUnitRunner.class)
public final class MapDownloadControllerTest {
  @Mock
  private TutorialMapPreferences tutorialMapPreferences;

  @Mock
  private UserMaps userMaps;

  @Test
  public void testShouldPromptToDownloadTutorialMap_ShouldReturnTrueWhenCanPromptToDownloadAndUserMapsIsEmpty() {
    when(tutorialMapPreferences.canPromptToDownload()).thenReturn(true);
    when(userMaps.isEmpty()).thenReturn(true);

    assertThat(shouldPromptToDownloadTutorialMap(), is(true));
  }

  private boolean shouldPromptToDownloadTutorialMap() {
    return MapDownloadController.shouldPromptToDownloadTutorialMap(tutorialMapPreferences, userMaps);
  }

  @Test
  public void testShouldPromptToDownloadTutorialMap_ShouldReturnFalseWhenCanPromptToDownloadAndUserMapsIsNotEmpty() {
    when(tutorialMapPreferences.canPromptToDownload()).thenReturn(true);
    when(userMaps.isEmpty()).thenReturn(false);

    assertThat(shouldPromptToDownloadTutorialMap(), is(false));
  }

  @Test
  public void testShouldPromptToDownloadTutorialMap_ShouldReturnFalseWhenCannotPromptToDownload() {
    when(tutorialMapPreferences.canPromptToDownload()).thenReturn(false);

    assertThat(shouldPromptToDownloadTutorialMap(), is(false));
  }

  @Test
  public void testPreventPromptToDownloadTutorialMap_ShouldChangePreventPromptToDownloadPreference() {
    preventPromptToDownloadTutorialMap();

    verify(tutorialMapPreferences).preventPromptToDownload();
  }

  private void preventPromptToDownloadTutorialMap() {
    MapDownloadController.preventPromptToDownloadTutorialMap(tutorialMapPreferences);
  }
}
