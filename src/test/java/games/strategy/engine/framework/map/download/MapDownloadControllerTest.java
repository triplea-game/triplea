package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.experimental.extensions.MockitoExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;

import games.strategy.engine.framework.map.download.MapDownloadController.DownloadedMaps;
import games.strategy.engine.framework.map.download.MapDownloadController.TutorialMapPreferences;
import games.strategy.engine.framework.map.download.MapDownloadController.UserMaps;
import games.strategy.util.Version;

public final class MapDownloadControllerTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  public final class GetOutOfDateMapNamesTest {
    private final String mapName = "myMap";

    private final File mapZipFile1 = new File("file1.zip");

    private final File mapZipFile2 = new File("file2.zip");

    private final Version version1 = new Version(1, 0);

    private final Version version2 = new Version(2, 0);

    private final Version versionUnknown = null;

    @Mock
    private DownloadedMaps downloadedMaps;

    @Test
    public void shouldIncludeMapWhenMapIsOutOfDate() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(version2);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, contains(mapName));
    }

    private Collection<DownloadFileDescription> givenLatestMapVersionIs(final Version version) {
      return givenDownload(newDownloadWithVersion(version));
    }

    private Collection<DownloadFileDescription> givenDownload(final DownloadFileDescription download) {
      return Collections.singletonList(download);
    }

    private DownloadFileDescription newDownloadWithVersion(final Version version) {
      return new DownloadFileDescription(
          "url",
          "description",
          mapName,
          version,
          DownloadFileDescription.DownloadType.MAP,
          DownloadFileDescription.MapCategory.BEST);
    }

    private void givenDownloadedMapVersionIs(final Version version) {
      when(downloadedMaps.getZipFileCandidates(mapName)).thenReturn(Arrays.asList(mapZipFile1, mapZipFile2));
      doReturn(Optional.empty()).when(downloadedMaps).getVersionForZipFile(mapZipFile1);
      doReturn(Optional.of(version)).when(downloadedMaps).getVersionForZipFile(mapZipFile2);
    }

    private Collection<String> getOutOfDateMapNames(final Collection<DownloadFileDescription> downloads) {
      return MapDownloadController.getOutOfDateMapNames(downloads, downloadedMaps);
    }

    @Test
    public void shouldExcludeMapWhenMapIsUpToDate() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(version1);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(mapName)));
    }

    @Test
    public void shouldExcludeMapWhenDownloadedVersionIsUnknown() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(version1);
      givenDownloadedMapVersionIsUnknown();

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(mapName)));
    }

    private void givenDownloadedMapVersionIsUnknown() {
      when(downloadedMaps.getZipFileCandidates(mapName)).thenReturn(Arrays.asList(mapZipFile1, mapZipFile2));
      when(downloadedMaps.getVersionForZipFile(any())).thenReturn(Optional.empty());
    }

    @Test
    public void shouldExcludeMapWhenLatestVersionIsUnknown() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(versionUnknown);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(mapName)));
    }

    @Test
    public void shouldExcludeMapWhenDownloadIsNull() {
      final Collection<DownloadFileDescription> downloads = givenDownload(null);
      givenDownloadedMapVersionIs(version1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, empty());
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class PreventPromptToDownloadTutorialMapTest {
    @Mock
    private TutorialMapPreferences tutorialMapPreferences;

    @Test
    public void shouldChangePreventPromptToDownloadPreference() {
      preventPromptToDownloadTutorialMap();

      verify(tutorialMapPreferences).preventPromptToDownload();
    }

    private void preventPromptToDownloadTutorialMap() {
      MapDownloadController.preventPromptToDownloadTutorialMap(tutorialMapPreferences);
    }
  }

  @ExtendWith(MockitoExtension.class)
  @Nested
  public final class ShouldPromptToDownloadTutorialMapTest {
    @Mock
    private TutorialMapPreferences tutorialMapPreferences;

    @Mock
    private UserMaps userMaps;

    @Test
    public void shouldReturnTrueWhenCanPromptToDownloadAndUserMapsIsEmpty() {
      when(tutorialMapPreferences.canPromptToDownload()).thenReturn(true);
      when(userMaps.isEmpty()).thenReturn(true);

      assertThat(shouldPromptToDownloadTutorialMap(), is(true));
    }

    private boolean shouldPromptToDownloadTutorialMap() {
      return MapDownloadController.shouldPromptToDownloadTutorialMap(tutorialMapPreferences, userMaps);
    }

    @Test
    public void shouldReturnFalseWhenCanPromptToDownloadAndUserMapsIsNotEmpty() {
      when(tutorialMapPreferences.canPromptToDownload()).thenReturn(true);
      when(userMaps.isEmpty()).thenReturn(false);

      assertThat(shouldPromptToDownloadTutorialMap(), is(false));
    }

    @Test
    public void shouldReturnFalseWhenCannotPromptToDownload() {
      when(tutorialMapPreferences.canPromptToDownload()).thenReturn(false);

      assertThat(shouldPromptToDownloadTutorialMap(), is(false));
    }
  }
}
