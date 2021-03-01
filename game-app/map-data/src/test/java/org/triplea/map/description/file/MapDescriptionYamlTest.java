package org.triplea.map.description.file;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresentAndIs;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
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
                        .xmlFileName("path.xml")
                        .build()))
            .build());
  }

  @Test
  void getGameXmlPathByGameName() throws Exception {
    final Path mapFolder =
        Path.of(
            MapDescriptionYamlTest.class.getClassLoader().getResource("map_yml_example").toURI());
    final Path ymlFile = mapFolder.resolve(MapDescriptionYaml.MAP_YAML_FILE_NAME);

    final MapDescriptionYaml mapDescriptionYaml =
        MapDescriptionYaml.fromFile(ymlFile.toFile())
            .orElseThrow(
                () -> new IllegalStateException("Unexpected failure to parse map.yml file"));

    final Optional<Path> gameFilePath = mapDescriptionYaml.getGameXmlPathByGameName("Great Game");
    assertThat(
        gameFilePath,
        isPresentAndIs(mapFolder.resolve("games").resolve("game1").resolve("game-file.xml")));
  }
}
