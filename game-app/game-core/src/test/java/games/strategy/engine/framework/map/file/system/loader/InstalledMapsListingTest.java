package games.strategy.engine.framework.map.file.system.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.triplea.test.common.matchers.CollectionMatchers.containsMappedItem;
import static org.triplea.test.common.matchers.CollectionMatchers.doesNotContainMappedItem;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.map.description.file.MapDescriptionYaml;

class InstalledMapsListingTest {

  final InstalledMapsListing installedMapsListing =
      new InstalledMapsListing(
          List.of(
              InstalledMap.builder()
                  .mapDescriptionYaml(
                      MapDescriptionYaml.builder()
                          .yamlFileLocation(Path.of("/path/map0/map.yml").toUri())
                          .mapName("map-name0")
                          .game(
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("aGame0")
                                  .xmlFileName("path.xml")
                                  .build())
                          .build())
                  .lastModifiedDate(Instant.now())
                  .build(),
              InstalledMap.builder()
                  .mapDescriptionYaml(
                      MapDescriptionYaml.builder()
                          .yamlFileLocation(Path.of("/path/map1/map.yml").toUri())
                          .mapName("map-name1")
                          .game(
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("gameName0")
                                  .xmlFileName("path0.xml")
                                  .build())
                          .game(
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("gameName1")
                                  .xmlFileName("path1.xml")
                                  .build())
                          .build())
                  .lastModifiedDate(Instant.now())
                  .build()));

  @Test
  void getSortedGamesList() {
    final List<String> sortedGameNames = installedMapsListing.getSortedGameList();

    assertThat(sortedGameNames, hasSize(3));
    assertThat(sortedGameNames, hasItems("aGame0", "gameName0", "gameName1"));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "DNE",
        "map_name", // does not quite match, missing trailing 0
        "map-name", // does not quite match, missing trailing 0
        "map",
        "name0"
      })
  void isMapInstalled_NegativeCases(final String mapName) {
    assertThat(installedMapsListing.isMapInstalled(mapName), is(false));
  }

  /** Verify match is not case sensitive with insignificant characters ignored */
  @ParameterizedTest
  @ValueSource(
      strings = {
        "MAP NAME0",
        "MAP_NAME0",
        "MAP-NAME0",
        "map name0",
        "map_name0",
        "map-name0",
        "mapname0",
        "MAPNAME0"
      })
  void isMapInstalled_PositiveCases(final String mapName) {
    assertThat(installedMapsListing.isMapInstalled(mapName), is(true));
  }

  /**
   * We have 2 installed maps and are presented with a list of updates for those maps. Both
   * installed maps have an updated date of 'now', one of the maps available is updated 3 months
   * ago, and the other 3 months in the future. The map with an available future update should show
   * up as out of date (there is a newer version available for download).
   */
  @Test
  void getOutODate() {
    // map-name0 -> most recent version available is older
    // map-name1 -> most recent version available is newer

    final Map<MapDownloadItem, InstalledMap> results =
        installedMapsListing.findOutOfDateMaps(
            List.of(
                MapDownloadItem.builder()
                    .mapName("map-name0")
                    .lastCommitDateEpochMilli(
                        Instant.now().minus(90, ChronoUnit.DAYS).toEpochMilli())
                    .downloadUrl("url")
                    .description("description")
                    .previewImageUrl("url")
                    .build(),
                MapDownloadItem.builder()
                    .mapName("map-name1")
                    .lastCommitDateEpochMilli(
                        Instant.now().plus(90, ChronoUnit.DAYS).toEpochMilli())
                    .downloadUrl("url")
                    .description("description")
                    .previewImageUrl("url")
                    .build()));

    assertThat(results.values(), doesNotContainMappedItem(InstalledMap::getMapName, "map-name0"));
    assertThat(results.values(), containsMappedItem(InstalledMap::getMapName, "map-name1"));
  }
}
