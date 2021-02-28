package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.map.download.MapDownloadController.TutorialMapPreferences;
import games.strategy.engine.framework.map.download.MapDownloadController.UserMaps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

final class MapDownloadControllerTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class PreventPromptToDownloadTutorialMapTest {
    @Mock private TutorialMapPreferences tutorialMapPreferences;

    @Test
    void shouldChangePreventPromptToDownloadPreference() {
      preventPromptToDownloadTutorialMap();

      verify(tutorialMapPreferences).preventPromptToDownload();
    }

    private void preventPromptToDownloadTutorialMap() {
      MapDownloadController.preventPromptToDownloadTutorialMap(tutorialMapPreferences);
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  final class ShouldPromptToDownloadTutorialMapTest {
    @Mock private TutorialMapPreferences tutorialMapPreferences;

    @Mock private UserMaps userMaps;

    @Test
    void shouldReturnTrueWhenCanPromptToDownloadAndUserMapsIsEmpty() {
      when(tutorialMapPreferences.canPromptToDownload()).thenReturn(true);
      when(userMaps.isEmpty()).thenReturn(true);

      assertThat(shouldPromptToDownloadTutorialMap(), is(true));
    }

    private boolean shouldPromptToDownloadTutorialMap() {
      return MapDownloadController.shouldPromptToDownloadTutorialMap(
          tutorialMapPreferences, userMaps);
    }

    @Test
    void shouldReturnFalseWhenCanPromptToDownloadAndUserMapsIsNotEmpty() {
      when(tutorialMapPreferences.canPromptToDownload()).thenReturn(true);
      when(userMaps.isEmpty()).thenReturn(false);

      assertThat(shouldPromptToDownloadTutorialMap(), is(false));
    }

    @Test
    void shouldReturnFalseWhenCannotPromptToDownload() {
      when(tutorialMapPreferences.canPromptToDownload()).thenReturn(false);

      assertThat(shouldPromptToDownloadTutorialMap(), is(false));
    }
  }
}
