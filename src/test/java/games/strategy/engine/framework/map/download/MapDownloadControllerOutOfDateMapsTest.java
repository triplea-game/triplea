package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.framework.map.download.MapDownloadController.DownloadedMaps;
import games.strategy.util.Version;

@RunWith(MockitoJUnitRunner.class)
public final class MapDownloadControllerOutOfDateMapsTest {
  private static final String MAP_NAME = "myMap";

  private static final File MAP_ZIP_FILE_1 = new File("file1.zip");

  private static final File MAP_ZIP_FILE_2 = new File("file2.zip");

  private static final Version VERSION_1 = new Version(1, 0);

  private static final Version VERSION_2 = new Version(2, 0);

  private static final Version VERSION_UNKNOWN = null;

  @Mock
  private DownloadedMaps downloadedMaps;

  @Test
  public void getOutOfDateMapNames_ShouldIncludeMapWhenMapIsOutOfDate() {
    final Collection<DownloadFileDescription> downloadFileDescriptions = givenLatestMapVersionIs(VERSION_2);
    givenDownloadedMapVersionIs(VERSION_1);

    final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloadFileDescriptions);

    assertThat(outOfDateMapNames, contains(MAP_NAME));
  }

  private static Collection<DownloadFileDescription> givenLatestMapVersionIs(final Version version) {
    return givenDownloadFileDescription(newDownloadFileDescriptionWithVersion(version));
  }

  private static Collection<DownloadFileDescription> givenDownloadFileDescription(
      final DownloadFileDescription downloadFileDescription) {
    return Collections.singletonList(downloadFileDescription);
  }

  private static DownloadFileDescription newDownloadFileDescriptionWithVersion(final Version version) {
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

  private Collection<String> getOutOfDateMapNames(final Collection<DownloadFileDescription> downloadFileDescriptions) {
    return MapDownloadController.getOutOfDateMapNames(downloadFileDescriptions, downloadedMaps);
  }

  @Test
  public void getOutOfDateMapNames_ShouldExcludeMapWhenMapIsUpToDate() {
    final Collection<DownloadFileDescription> downloadFileDescriptions = givenLatestMapVersionIs(VERSION_1);
    givenDownloadedMapVersionIs(VERSION_1);

    final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloadFileDescriptions);

    assertThat(outOfDateMapNames, not(contains(MAP_NAME)));
  }

  @Test
  public void getOutOfDateMapNames_ShouldExcludeMapWhenDownloadedVersionIsUnknown() {
    final Collection<DownloadFileDescription> downloadFileDescriptions = givenLatestMapVersionIs(VERSION_1);
    givenDownloadedMapVersionIsUnknown();

    final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloadFileDescriptions);

    assertThat(outOfDateMapNames, not(contains(MAP_NAME)));
  }

  private void givenDownloadedMapVersionIsUnknown() {
    when(downloadedMaps.getZipFileCandidates(MAP_NAME)).thenReturn(Arrays.asList(MAP_ZIP_FILE_1, MAP_ZIP_FILE_2));
    when(downloadedMaps.getVersionForZipFile(any())).thenReturn(Optional.empty());
  }

  @Test
  public void getOutOfDateMapNames_ShouldExcludeMapWhenLatestVersionIsUnknown() {
    final Collection<DownloadFileDescription> downloadFileDescriptions = givenLatestMapVersionIs(VERSION_UNKNOWN);
    givenDownloadedMapVersionIs(VERSION_1);

    final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloadFileDescriptions);

    assertThat(outOfDateMapNames, not(contains(MAP_NAME)));
  }

  @Test
  public void getOutOfDateMapNames_ShouldExcludeMapWhenDownloadFileDescriptionIsNull() {
    final Collection<DownloadFileDescription> downloadFileDescriptions = givenDownloadFileDescription(null);
    givenDownloadedMapVersionIs(VERSION_1);

    final Collection<String> outOfDateMapNames = getOutOfDateMapNames(downloadFileDescriptions);

    assertThat(outOfDateMapNames, empty());
  }
}
