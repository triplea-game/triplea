package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import games.strategy.engine.framework.map.file.system.loader.DownloadedMap;
import games.strategy.engine.framework.map.file.system.loader.DownloadedMapsListing;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.triplea.map.description.file.MapDescriptionYaml;

class AvailableMapsListingTest extends AbstractClientSettingTestCase {
  private static final String MAP_NAME = "new_test_order";
  private static final int MAP_VERSION = 10;

  private static final DownloadFileDescription TEST_MAP =
      DownloadFileDescription.builder()
          .url("")
          .mapName(MAP_NAME)
          .version(MAP_VERSION)
          .mapCategory(DownloadFileDescription.MapCategory.EXPERIMENTAL)
          .build();

  @Test
  void testAvailable() {
    final DownloadedMapsListing downloadedMapsListing = new DownloadedMapsListing(List.of());

    final AvailableMapsListing availableMapsListing =
        new AvailableMapsListing(List.of(TEST_MAP), downloadedMapsListing);

    assertThat(availableMapsListing.getAvailable(), hasSize(1));
    assertThat(availableMapsListing.getInstalled(), is(empty()));
    assertThat(availableMapsListing.getOutOfDate(), is(empty()));
  }

  @Test
  void testAvailableExcluding() {
    final DownloadedMapsListing downloadedMapsListing = new DownloadedMapsListing(List.of());

    final DownloadFileDescription download1 = newDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newDownloadWithUrl("url3");
    final AvailableMapsListing availableMapsListing =
        new AvailableMapsListing(List.of(download1, download2, download3), downloadedMapsListing);

    final List<DownloadFileDescription> available =
        availableMapsListing.getAvailableExcluding(List.of(download1, download3));

    assertThat(available, is(List.of(download2)));
  }

  private static DownloadFileDescription newDownloadWithUrl(final String url) {
    return DownloadFileDescription.builder()
        .url(url)
        .description("description")
        .mapName("mapName " + url)
        .version(MAP_VERSION)
        .mapCategory(DownloadFileDescription.MapCategory.BEST)
        .build();
  }

  private static DownloadFileDescription newInstalledDownloadWithUrl(final String url) {
    return DownloadFileDescription.builder()
        .url(url)
        .description("description")
        .mapName("mapName " + url)
        .version(MAP_VERSION)
        .mapCategory(DownloadFileDescription.MapCategory.BEST)
        .installLocation(new File("/"))
        .build();
  }

  @Test
  void testInstalled() {
    final DownloadedMapsListing downloadedMapsListing =
        buildIndexWithMapVersions(Map.of("mapName url", MAP_VERSION));

    final AvailableMapsListing availableMapsListing =
        new AvailableMapsListing(
            List.of(newInstalledDownloadWithUrl("url")), downloadedMapsListing);

    assertThat(availableMapsListing.getAvailable(), is(empty()));
    assertThat(availableMapsListing.getInstalled(), hasSize(1));
    assertThat(availableMapsListing.getOutOfDate(), is(empty()));
  }

  private static DownloadedMapsListing buildIndexWithMapVersions(
      final Map<String, Integer> mapNameToVersion) {

    return new DownloadedMapsListing(
        mapNameToVersion.entrySet().stream()
            .map(
                entry ->
                    new DownloadedMap(
                        MapDescriptionYaml.builder()
                            .yamlFileLocation(new File("/local/file").toURI())
                            .mapName(entry.getKey())
                            .mapVersion(entry.getValue())
                            .mapGameList(
                                List.of(
                                    MapDescriptionYaml.MapGame.builder()
                                        .xmlPath("path.xml")
                                        .gameName("game")
                                        .build()))
                            .build()))
            .collect(Collectors.toList()));
  }

  @Test
  void testOutOfDate() {
    final DownloadedMapsListing downloadedMapsListing =
        buildIndexWithMapVersions(Map.of("mapName url", MAP_VERSION - 1));
    final AvailableMapsListing availableMapsListing =
        new AvailableMapsListing(
            List.of(newInstalledDownloadWithUrl("url")), downloadedMapsListing);

    assertThat(availableMapsListing.getAvailable(), is(empty()));
    assertThat(availableMapsListing.getInstalled(), is(empty()));
    assertThat(availableMapsListing.getOutOfDate(), hasSize(1));
  }

  @Test
  void testOutOfDateExcluding() {
    final DownloadFileDescription download1 = newInstalledDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newInstalledDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newInstalledDownloadWithUrl("url3");

    final DownloadedMapsListing downloadedMapsListing =
        buildIndexWithMapVersions(
            Map.of(
                download1.getMapName(), download1.getVersion() - 1,
                download2.getMapName(), download2.getVersion() - 1,
                download3.getMapName(), download3.getVersion() - 1));

    final AvailableMapsListing availableMapsListing =
        new AvailableMapsListing(List.of(download1, download2, download3), downloadedMapsListing);

    final List<DownloadFileDescription> outOfDate =
        availableMapsListing.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }
}
