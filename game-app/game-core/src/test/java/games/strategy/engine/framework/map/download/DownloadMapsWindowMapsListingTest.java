package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.map.description.file.MapDescriptionYaml;

class DownloadMapsWindowMapsListingTest extends AbstractClientSettingTestCase {
  private static final String MAP_NAME = "new_test_order";
  private static final int MAP_VERSION = 10;

  private static final MapDownloadItem TEST_MAP =
      MapDownloadItem.builder()
          .downloadUrl("")
          .previewImageUrl("")
          .mapName(MAP_NAME)
          .version(MAP_VERSION)
          .description("description")
          .lastCommitDateEpochMilli(10L)
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
        .version(MAP_VERSION)
        .lastCommitDateEpochMilli(40L)
        .build();
  }

  private static MapDownloadItem newInstalledDownloadWithUrl(final String url) {
    return MapDownloadItem.builder()
        .downloadUrl(url)
        .previewImageUrl(url)
        .description("description")
        .mapName("mapName " + url)
        .version(MAP_VERSION)
        .lastCommitDateEpochMilli(30L)
        .build();
  }

  @Test
  void testInstalled() {
    final InstalledMapsListing installedMapsListing =
        buildIndexWithMapVersions(Map.of("mapName url", MAP_VERSION));

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(newInstalledDownloadWithUrl("url")), installedMapsListing);

    assertThat(downloadMapsWindowMapsListing.getAvailable(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getInstalled().entrySet(), hasSize(1));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate().entrySet(), is(empty()));
  }

  private static InstalledMapsListing buildIndexWithMapVersions(
      final Map<String, Integer> mapNameToVersion) {

    return new InstalledMapsListing(
        mapNameToVersion.entrySet().stream()
            .map(
                entry ->
                    new InstalledMap(
                        MapDescriptionYaml.builder()
                            .yamlFileLocation(Path.of("/local/file").toUri())
                            .mapName(entry.getKey())
                            .mapVersion(entry.getValue())
                            .mapGameList(
                                List.of(
                                    MapDescriptionYaml.MapGame.builder()
                                        .xmlFileName("path.xml")
                                        .gameName("game")
                                        .build()))
                            .build()))
            .collect(Collectors.toList()));
  }

  @Test
  void testOutOfDate() {
    final InstalledMapsListing installedMapsListing =
        buildIndexWithMapVersions(Map.of("mapName url", MAP_VERSION - 1));
    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(newInstalledDownloadWithUrl("url")), installedMapsListing);

    assertThat(downloadMapsWindowMapsListing.getAvailable(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getInstalled().entrySet(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate().entrySet(), hasSize(1));
  }

  @Test
  void testOutOfDateExcluding() {
    final MapDownloadItem download1 = newInstalledDownloadWithUrl("url1");
    final MapDownloadItem download2 = newInstalledDownloadWithUrl("url2");
    final MapDownloadItem download3 = newInstalledDownloadWithUrl("url3");

    final InstalledMapsListing installedMapsListing =
        buildIndexWithMapVersions(
            Map.of(
                download1.getMapName(), download1.getVersion() - 1,
                download2.getMapName(), download2.getVersion() - 1,
                download3.getMapName(), download3.getVersion() - 1));

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(download1, download2, download3), installedMapsListing);

    final List<MapDownloadItem> outOfDate =
        downloadMapsWindowMapsListing.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }
}
