package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NonNls;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.map.description.file.MapDescriptionYaml;

class DownloadMapsWindowMapsListingTest extends AbstractClientSettingTestCase {
  @NonNls private static final String MAP_NAME = "new_test_order";

  private static final MapDownloadItem TEST_MAP =
      MapDownloadItem.builder()
          .downloadUrl("")
          .previewImageUrl("")
          .mapName(MAP_NAME)
          .description("description")
          .lastCommitDateEpochMilli(10L)
          .downloadSizeInBytes(750L)
          .build();

  @Test
  void testAvailable() {
    final InstalledMapsListing installedMapsListing = new InstalledMapsListing(List.of());

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(List.of(TEST_MAP), installedMapsListing);

    assertThat(downloadMapsWindowMapsListing.getAvailable(), hasSize(1));
    assertThat(downloadMapsWindowMapsListing.getInstalled().entrySet(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate().entrySet(), is(empty()));
  }

  @Test
  void testAvailableExcluding() {
    final InstalledMapsListing installedMapsListing = new InstalledMapsListing(List.of());

    final MapDownloadItem download1 = newDownloadWithUrl("url1");
    final MapDownloadItem download2 = newDownloadWithUrl("url2");
    final MapDownloadItem download3 = newDownloadWithUrl("url3");
    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(download1, download2, download3), installedMapsListing);

    final List<MapDownloadItem> available =
        downloadMapsWindowMapsListing.getAvailableExcluding(List.of(download1, download3));

    assertThat(available, is(List.of(download2)));
  }

  private static MapDownloadItem newDownloadWithUrl(final String url) {
    return MapDownloadItem.builder()
        .downloadUrl(url)
        .previewImageUrl(url)
        .description("description")
        .mapName("mapName " + url)
        .lastCommitDateEpochMilli(Instant.now().toEpochMilli())
        .lastCommitDateEpochMilli(40L)
        .downloadSizeInBytes(500L)
        .build();
  }

  /**
   * - List of maps installed contains 1 <br>
   * - List of available maps contains same 1 with an older last update date
   *
   * <p>Expecting this map to be listed as installed (and otherwise up-to-date).
   */
  @Test
  void testInstalled() {
    final MapDownloadItem installed = newDownloadWithUrl("url");

    final InstalledMapsListing installedMapsListing =
        buildInstalledMapsListing(
            Map.of(installed.getMapName(), installed.getLastCommitDateEpochMilli() + 30));

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(List.of(installed), installedMapsListing);

    assertThat(downloadMapsWindowMapsListing.getAvailable(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getInstalled().entrySet(), hasSize(1));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate().entrySet(), is(empty()));
  }

  private static InstalledMapsListing buildInstalledMapsListing(
      final Map<String, Long> mapNameToVersion) {

    return new InstalledMapsListing(
        mapNameToVersion.entrySet().stream()
            .map(
                entry ->
                    InstalledMap.builder()
                        .lastModifiedDate(Instant.ofEpochMilli(entry.getValue()))
                        .mapDescriptionYaml(
                            MapDescriptionYaml.builder()
                                .yamlFileLocation(Path.of("/local/file"))
                                .mapName(entry.getKey())
                                .mapGameList(
                                    List.of(
                                        MapDescriptionYaml.MapGame.builder()
                                            .xmlFileName("path.xml")
                                            .gameName("game")
                                            .build()))
                                .build())
                        .build())
            .collect(Collectors.toList()));
  }

  /**
   * - List of maps installed contains 1 <br>
   * - List of available maps contains same 1 with a newer last update date
   *
   * <p>Expecting this map to be listed as out of date.
   */
  @Test
  void testOutOfDate() {
    final MapDownloadItem installed = newDownloadWithUrl("url");

    final InstalledMapsListing installedMapsListing =
        buildInstalledMapsListing(
            Map.of(installed.getMapName(), installed.getLastCommitDateEpochMilli() - 30));

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(List.of(installed), installedMapsListing);

    assertThat(downloadMapsWindowMapsListing.getAvailable(), is(empty()));
    assertThat(
        "we expect the one map available for download to be detected as installed",
        downloadMapsWindowMapsListing.getInstalled().entrySet(),
        hasSize(1));
    assertThat(
        "we expect the one map available for download to be detected as out of date",
        downloadMapsWindowMapsListing.getOutOfDate().entrySet(),
        hasSize(1));
  }

  @Test
  void testOutOfDateExcluding() {
    final MapDownloadItem download1 = newDownloadWithUrl("url1");
    final MapDownloadItem download2 = newDownloadWithUrl("url2");
    final MapDownloadItem download3 = newDownloadWithUrl("url3");

    final InstalledMapsListing installedMapsListing =
        buildInstalledMapsListing(
            Map.of(
                download1.getMapName(), download1.getLastCommitDateEpochMilli() - 10,
                download2.getMapName(), download1.getLastCommitDateEpochMilli() - 10,
                download3.getMapName(), download1.getLastCommitDateEpochMilli() - 10));

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(download1, download2, download3), installedMapsListing);

    final List<MapDownloadItem> outOfDate =
        downloadMapsWindowMapsListing.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }
}
