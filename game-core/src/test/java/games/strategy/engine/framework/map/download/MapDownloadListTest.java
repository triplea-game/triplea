package games.strategy.engine.framework.map.download;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;

import games.strategy.engine.framework.map.file.system.loader.DownloadedMaps;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.triplea.map.description.file.MapDescriptionYaml;

class MapDownloadListTest extends AbstractClientSettingTestCase {
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
    final DownloadedMaps downloadedMaps = new DownloadedMaps(List.of());

    final MapDownloadList mapDownloadList = new MapDownloadList(List.of(TEST_MAP), downloadedMaps);

    assertThat(mapDownloadList.getAvailable(), hasSize(1));
    assertThat(mapDownloadList.getInstalled(), is(empty()));
    assertThat(mapDownloadList.getOutOfDate(), is(empty()));
  }

  @Test
  void testAvailableExcluding() {
    final DownloadedMaps downloadedMaps = new DownloadedMaps(List.of());

    final DownloadFileDescription download1 = newDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newDownloadWithUrl("url3");
    final MapDownloadList mapDownloadList =
        new MapDownloadList(List.of(download1, download2, download3), downloadedMaps);

    final List<DownloadFileDescription> available =
        mapDownloadList.getAvailableExcluding(List.of(download1, download3));

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
    final DownloadedMaps downloadedMaps =
        buildIndexWithMapVersions(Map.of("mapName url", MAP_VERSION));

    final MapDownloadList mapDownloadList =
        new MapDownloadList(List.of(newInstalledDownloadWithUrl("url")), downloadedMaps);

    assertThat(mapDownloadList.getAvailable(), is(empty()));
    assertThat(mapDownloadList.getInstalled(), hasSize(1));
    assertThat(mapDownloadList.getOutOfDate(), is(empty()));
  }

  private static DownloadedMaps buildIndexWithMapVersions(
      final Map<String, Integer> mapNameToVersion) {

    return new DownloadedMaps(
        mapNameToVersion.entrySet().stream()
            .map(
                entry ->
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
                        .build())
            .collect(Collectors.toList()));
  }

  @Test
  void testOutOfDate() {
    final DownloadedMaps downloadedMaps =
        buildIndexWithMapVersions(Map.of("mapName url", MAP_VERSION - 1));
    final MapDownloadList mapDownloadList =
        new MapDownloadList(List.of(newInstalledDownloadWithUrl("url")), downloadedMaps);

    assertThat(mapDownloadList.getAvailable(), is(empty()));
    assertThat(mapDownloadList.getInstalled(), is(empty()));
    assertThat(mapDownloadList.getOutOfDate(), hasSize(1));
  }

  @Test
  void testOutOfDateExcluding() {
    final DownloadFileDescription download1 = newInstalledDownloadWithUrl("url1");
    final DownloadFileDescription download2 = newInstalledDownloadWithUrl("url2");
    final DownloadFileDescription download3 = newInstalledDownloadWithUrl("url3");

    final DownloadedMaps downloadedMaps =
        buildIndexWithMapVersions(
            Map.of(
                download1.getMapName(), download1.getVersion() - 1,
                download2.getMapName(), download2.getVersion() - 1,
                download3.getMapName(), download3.getVersion() - 1));

    final MapDownloadList mapDownloadList =
        new MapDownloadList(List.of(download1, download2, download3), downloadedMaps);

    final List<DownloadFileDescription> outOfDate =
        mapDownloadList.getOutOfDateExcluding(List.of(download1, download3));

    assertThat(outOfDate, is(List.of(download2)));
  }
}
