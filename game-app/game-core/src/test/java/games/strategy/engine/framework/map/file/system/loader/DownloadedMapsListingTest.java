package games.strategy.engine.framework.map.file.system.loader;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

import java.io.File;
import java.nio.file.Path;
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
                                  .xmlPath("games/path.xml")
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
                                  .xmlPath("path0.xml")
                                  .build(),
                              MapDescriptionYaml.MapGame.builder()
                                  .gameName("gameName1")
                                  .xmlPath("path1.xml")
                                  .build()))
                      .build())));

  @Test
  void getSortedGamesList() {
    final List<String> sortedGameNames = downloadedMapsListing.getSortedGameList();

    assertThat(sortedGameNames, hasSize(3));
    assertThat(sortedGameNames, hasItems("aGame0", "gameName0", "gameName1"));
  }

  @Test
  void findGameXmlPathByGameName() {
    assertThat(
        downloadedMapsListing.findGameXmlPathByGameName("aGame0"),
        isPresentAndIs(Path.of("/path/map0/games/path.xml")));
    assertThat(
        downloadedMapsListing.findGameXmlPathByGameName("gameName0"),
        isPresentAndIs(Path.of("/path/map1/path0.xml")));
    assertThat(
        downloadedMapsListing.findGameXmlPathByGameName("gameName1"),
        isPresentAndIs(Path.of("/path/map1/path1.xml")));
  }

  @Test
  void hasGame() {
    assertThat(downloadedMapsListing.hasGame("DNE"), is(false));
    assertThat(downloadedMapsListing.hasGame("aGame0"), is(true));
    assertThat(downloadedMapsListing.hasGame("gameName0"), is(true));
    assertThat(downloadedMapsListing.hasGame("gameName1"), is(true));
  }

  @Test
  void getMapVersionByName() {
    assertThat(downloadedMapsListing.getMapVersionByName("DNE"), is(0));
    assertThat(downloadedMapsListing.getMapVersionByName("map-name0"), is(0));
    assertThat(downloadedMapsListing.getMapVersionByName("map-name1"), is(2));
  }
}
