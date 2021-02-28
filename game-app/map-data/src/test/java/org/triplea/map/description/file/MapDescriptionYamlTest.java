package org.triplea.map.description.file;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import com.github.npathai.hamcrestopt.OptionalMatchers;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MapDescriptionYamlTest {

  @ParameterizedTest
  @MethodSource
  void isValid(final MapDescriptionYaml mapDescriptionYaml) {
    assertThat(mapDescriptionYaml.isValid(new File("")), is(true));
  }

  @SuppressWarnings("unused")
  static List<MapDescriptionYaml> isValid() {
    return List.of(
        MapDescriptionYaml.builder()
            .yamlFileLocation(new File("/path/on/disk/map.yml").toURI())
            .mapName("map name")
            .mapVersion(1)
            .mapGameList(
                List.of(
                    MapDescriptionYaml.MapGame.builder() //
                        .gameName("game name")
                        .xmlPath("path.xml")
                        .build()))
            .build());
  }

  @Test
  void getGameXmlPathByGameName() {
    final MapDescriptionYaml mapDescriptionYaml =
        MapDescriptionYaml.builder()
            .yamlFileLocation(new File("/my/path/map.yml").toURI())
            .mapName("map name")
            .mapVersion(1)
            .mapGameList(
                List.of(
                    MapDescriptionYaml.MapGame.builder() //
                        .gameName("game1")
                        .xmlPath("games/path1.xml")
                        .build(),
                    MapDescriptionYaml.MapGame.builder()
                        .gameName("game2")
                        .xmlPath("games/path2.xml")
                        .build()))
            .build();

    assertThat(
        mapDescriptionYaml.getGameXmlPathByGameName("game1"),
        isPresentAndIs(Path.of("/my/path/games/path1.xml")));
    assertThat(
        mapDescriptionYaml.getGameXmlPathByGameName("game2"),
        isPresentAndIs(Path.of("/my/path/games/path2.xml")));
    assertThat(
        "game name is not in the game list, looking up the game path by name is empty result.",
        mapDescriptionYaml.getGameXmlPathByGameName("game-DNE"),
        OptionalMatchers.isEmpty());
  }
}
