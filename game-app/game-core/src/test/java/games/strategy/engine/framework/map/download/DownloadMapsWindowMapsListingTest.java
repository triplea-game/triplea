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
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.map.description.file.MapDescriptionYaml;

class DownloadMapsWindowMapsListingTest extends AbstractClientSettingTestCase {
  private static final String MAP_NAME = "new_test_order";
  private static final int MAP_VERSION = 10;

  private static final MapDownloadListing TEST_MAP =
      MapDownloadListing.builder()
          .downloadUrl("")
          .mapName(MAP_NAME)
          .version(MAP_VERSION)
          .mapCategory("EXPERIMENTAL")
          .description("description")
          .build();

  @Test
  void testAvailable() {
    final InstalledMapsListing installedMapsListing = new InstalledMapsListing(List.of());

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(List.of(TEST_MAP), installedMapsListing);

    assertThat(downloadMapsWindowMapsListing.getAvailable(), hasSize(1));
    assertThat(downloadMapsWindowMapsListing.getInstalled(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate(), is(empty()));
  }

  @Test
  void testAvailableExcluding() {
    final InstalledMapsListing installedMapsListing = new InstalledMapsListing(List.of());

    final MapDownloadListing download1 = newDownloadWithUrl("url1");
    final MapDownloadListing download2 = newDownloadWithUrl("url2");
    final MapDownloadListing download3 = newDownloadWithUrl("url3");
    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(download1, download2, download3), installedMapsListing);

    final List<MapDownloadListing> available =
        downloadMapsWindowMapsListing.getAvailableExcluding(List.of(download1, download3));

    assertThat(available, is(List.of(download2)));
  }

  private static MapDownloadListing newDownloadWithUrl(final String url) {
    return MapDownloadListing.builder()
        .downloadUrl(url)
        .description("description")
        .mapName("mapName " + url)
        .version(MAP_VERSION)
        .mapCategory("BEST")
        .build();
  }

  private static MapDownloadListing newInstalledDownloadWithUrl(final String url) {
    return MapDownloadListing.builder()
        .downloadUrl(url)
        .description("description")
        .mapName("mapName " + url)
        .version(MAP_VERSION)
        .mapCategory("BEST")
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
    assertThat(downloadMapsWindowMapsListing.getInstalled(), hasSize(1));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate(), is(empty()));
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
    assertThat(downloadMapsWindowMapsListing.getInstalled(), is(empty()));
    assertThat(downloadMapsWindowMapsListing.getOutOfDate(), hasSize(1));
  }

  @Test
  void testOutOfDateExcluding() {
    final MapDownloadListing download1 = newInstalledDownloadWithUrl("url1");
    final MapDownloadListing download2 = newInstalledDownloadWithUrl("url2");
    final MapDownloadListing download3 = newInstalledDownloadWithUrl("url3");

    final InstalledMapsListing installedMapsListing =
        buildIndexWithMapVersions(
            Map.of(
                download1.getMapName(), download1.getVersion() - 1,
                download2.getMapName(), download2.getVersion() - 1,
                download3.getMapName(), download3.getVersion() - 1));

    final DownloadMapsWindowMapsListing downloadMapsWindowMapsListing =
        new DownloadMapsWindowMapsListing(
            List.of(download1, download2, download3), installedMapsListing);

    final List<MapDownloadListing> outOfDate =
        downloadMapsWindowMapsListing.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }
}
