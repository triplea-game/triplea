package org.triplea.map.description.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MapDescriptionYamlGeneratorTest {

  private Path exampleMapFolder;

  @BeforeEach
  void setup() throws Exception {
    exampleMapFolder =
        Path.of(
            MapDescriptionYamlGeneratorTest.class
                .getClassLoader()
                .getResource("map_description_yml_generator/example-map")
                .toURI());
  }

  // functionality under test
  private MapDescriptionYaml generateAndReadDescriptionYamlFile() {
    final Path generatedFile =
        MapDescriptionYamlGenerator.generateYamlDataForMap(exampleMapFolder).orElseThrow();
    generatedFile.toFile().deleteOnExit();

    // after generation of the YAML file, read it back so we can compare written data to expected
    return MapDescriptionYamlReader.readFromMap(exampleMapFolder).orElseThrow();
  }

  /** Verify data generated using a minimal map with properties file and two XML files. */
  @Test
  void verifyGeneration() {
    final MapDescriptionYaml mapDescriptionYaml = generateAndReadDescriptionYamlFile();

    assertThat(mapDescriptionYaml.getMapName(), is("example-map"));
    assertThat(mapDescriptionYaml.getMapGameList(), hasSize(2));

    assertThat(
        mapDescriptionYaml.getMapGameList().stream()
            .anyMatch(g -> g.getGameName().equals("example game")),
        is(true));

    assertThat(
        mapDescriptionYaml.getMapGameList().stream()
            .anyMatch(g -> g.getGameName().equals("example game 2")),
        is(true));

    assertThat(
        mapDescriptionYaml.getMapGameList().stream()
            .anyMatch(g -> g.getXmlFileName().equals("game.xml")),
        is(true));

    assertThat(
        mapDescriptionYaml.getMapGameList().stream()
            .anyMatch(g -> g.getXmlFileName().equals("game2.xml")),
        is(true));
  }
}
