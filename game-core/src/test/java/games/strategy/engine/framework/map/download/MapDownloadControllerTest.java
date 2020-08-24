package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.framework.map.download.MapDownloadController.DownloadedMaps;
import games.strategy.engine.framework.map.download.MapDownloadController.TutorialMapPreferences;
import games.strategy.engine.framework.map.download.MapDownloadController.UserMaps;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.util.Version;

final class MapDownloadControllerTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  final class GetOutOfDateMapNamesTest {
    private final String mapName = "myMap";

    private final File mapZipFile1 = new File("file1.zip");

    private final File mapZipFile2 = new File("file2.zip");

    private final Version version1 = new Version(1, 0, 0);

    private final Version version2 = new Version(2, 0, 0);

    private final Version versionUnknown = null;

    @Mock private DownloadedMaps downloadedMaps;

    @Test
    void shouldIncludeMapWhenMapIsOutOfDate() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(version2);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, contains(mapName));
    }

    private Collection<DownloadFileDescription> givenLatestMapVersionIs(final Version version) {
      return List.of(newDownloadWithVersion(version));
    }

    private DownloadFileDescription newDownloadWithVersion(final Version version) {
      return new DownloadFileDescription(
          "url",
          "description",
          mapName,
          version,
          DownloadFileDescription.DownloadType.MAP,
          DownloadFileDescription.MapCategory.BEST,
          "");
    }

    private void givenDownloadedMapVersionIs(final Version version) {
      when(downloadedMaps.getZipFileCandidates(mapName))
          .thenReturn(Arrays.asList(mapZipFile1, mapZipFile2));
      doReturn(Optional.empty()).when(downloadedMaps).getVersionForZipFile(mapZipFile1);
      doReturn(Optional.of(version)).when(downloadedMaps).getVersionForZipFile(mapZipFile2);
    }

    private Collection<String> getOutOfDateMapNames(
        final Collection<DownloadFileDescription> downloads) {
      return MapDownloadController.getOutOfDateMapNames(downloads, downloadedMaps);
    }

    @Test
    void shouldExcludeMapWhenMapIsUpToDate() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(version1);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(mapName)));
    }

    @Test
    void shouldExcludeMapWhenDownloadedVersionIsUnknown() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(version1);
      givenDownloadedMapVersionIsUnknown();

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(mapName)));
    }

    private void givenDownloadedMapVersionIsUnknown() {
      when(downloadedMaps.getZipFileCandidates(mapName))
          .thenReturn(Arrays.asList(mapZipFile1, mapZipFile2));
      when(downloadedMaps.getVersionForZipFile(any())).thenReturn(Optional.empty());
    }

    @Test
    void shouldExcludeMapWhenLatestVersionIsUnknown() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(versionUnknown);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(mapName)));
    }
  }

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
