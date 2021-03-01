package games.strategy.engine.framework.map.file.system.loader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

import java.io.File;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.map.description.file.MapDescriptionYaml;

class DownloadedMapsListingTest {

  final DownloadedMapsListing downloadedMapsListing =
      new DownloadedMapsListing(
          List.of(
              new DownloadedMap(
                  MapDescriptionYaml.builder()
                      .yamlFileLocation(new File("/path/map0/map.yml").toURI())
                      .mapName("map-name0")
                      .mapVersion(0)
                      .mapGameList(
                          List.of(
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("aGame0")
                                  .xmlFileName("path.xml")
                                  .build()))
                      .build()),
              new DownloadedMap(
                  MapDescriptionYaml.builder()
                      .yamlFileLocation(new File("/path/map1/map.yml").toURI())
                      .mapName("map-name1")
                      .mapVersion(2)
                      .mapGameList(
                          List.of(
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("gameName0")
                                  .xmlFileName("path0.xml")
                                  .build(),
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("gameName1")
                                  .xmlFileName("path1.xml")
                                  .build()))
                      .build())));

  @Test
  void getSortedGamesList() {
    final List<String> sortedGameNames = downloadedMapsListing.getSortedGameList();

    assertThat(sortedGameNames, hasSize(3));
    assertThat(sortedGameNames, hasItems("aGame0", "gameName0", "gameName1"));
  }

  @Test
  void getMapVersionByName() {
    assertThat(downloadedMapsListing.getMapVersionByName("DNE"), is(0));
    assertThat(downloadedMapsListing.getMapVersionByName("map-name0"), is(0));
    assertThat(downloadedMapsListing.getMapVersionByName("map-name1"), is(2));
  }
}
