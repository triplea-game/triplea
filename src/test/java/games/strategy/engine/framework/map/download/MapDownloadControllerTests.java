package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.MapDownloadController.DownloadedMaps;
import games.strategy.engine.framework.map.download.MapDownloadController.TutorialMapPreferences;
import games.strategy.engine.framework.map.download.MapDownloadController.UserMaps;
import games.strategy.util.Version;

@RunWith(Enclosed.class)
public final class MapDownloadControllerTests {
  @RunWith(MockitoJUnitRunner.class)
  public static final class GetOutOfDateMapNamesTest {
    private static final String MAP_NAME = "myMap";

    private static final File MAP_ZIP_FILE_1 = new File("file1.zip");

    private static final File MAP_ZIP_FILE_2 = new File("file2.zip");

    private static final Version VERSION_1 = new Version(1, 0);

    private static final Version VERSION_2 = new Version(2, 0);

    private static final Version VERSION_UNKNOWN = null;

    @Mock
    private DownloadedMaps downloadedMaps;

    @Test
    public void shouldIncludeMapWhenMapIsOutOfDate() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(VERSION_2);
      givenDownloadedMapVersionIs(VERSION_1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, contains(MAP_NAME));
    }

    private static Collection<DownloadFileDescription> givenLatestMapVersionIs(final Version version) {
      return givenDownload(newDownloadWithVersion(version));
    }

    private static Collection<DownloadFileDescription> givenDownload(final DownloadFileDescription download) {
      return Collections.singletonList(download);
    }

    private static DownloadFileDescription newDownloadWithVersion(final Version version) {
      return new DownloadFileDescription(
          "url",
          "description",
          MAP_NAME,
          version,
          DownloadFileDescription.DownloadType.MAP,
          DownloadFileDescription.MapCategory.BEST);
    }

    private void givenDownloadedMapVersionIs(final Version version) {
      when(downloadedMaps.getZipFileCandidates(MAP_NAME)).thenReturn(Arrays.asList(MAP_ZIP_FILE_1, MAP_ZIP_FILE_2));
      when(downloadedMaps.getVersionForZipFile(MAP_ZIP_FILE_1)).thenReturn(Optional.empty());
      when(downloadedMaps.getVersionForZipFile(MAP_ZIP_FILE_2)).thenReturn(Optional.of(version));
    }

    private Collection<String> getOutOfDateMapNames(final Collection<DownloadFileDescription> downloads) {
      return MapDownloadController.getOutOfDateMapNames(downloads, downloadedMaps);
    }

    @Test
    public void shouldExcludeMapWhenMapIsUpToDate() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(VERSION_1);
      givenDownloadedMapVersionIs(VERSION_1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(MAP_NAME)));
    }

    @Test
    public void shouldExcludeMapWhenDownloadedVersionIsUnknown() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(VERSION_1);
      givenDownloadedMapVersionIsUnknown();

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(MAP_NAME)));
    }

    private void givenDownloadedMapVersionIsUnknown() {
      when(downloadedMaps.getZipFileCandidates(MAP_NAME)).thenReturn(Arrays.asList(MAP_ZIP_FILE_1, MAP_ZIP_FILE_2));
      when(downloadedMaps.getVersionForZipFile(any())).thenReturn(Optional.empty());
    }

    @Test
    public void shouldExcludeMapWhenLatestVersionIsUnknown() {
      final Collection<DownloadFileDescription> downloads = givenLatestMapVersionIs(VERSION_UNKNOWN);
      givenDownloadedMapVersionIs(VERSION_1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, not(contains(MAP_NAME)));
    }

    @Test
    public void shouldExcludeMapWhenDownloadIsNull() {
      final Collection<DownloadFileDescription> downloads = givenDownload(null);
      givenDownloadedMapVersionIs(VERSION_1);

      final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloads);

      assertThat(outOfDateMapNames, empty());
    }
  }

  @RunWith(MockitoJUnitRunner.class)
  public static final class PreventPromptToDownloadTutorialMapTest {
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

  @RunWith(MockitoJUnitRunner.class)
  public static final class ShouldPromptToDownloadTutorialMapTest {
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
