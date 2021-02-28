package org.triplea.map.description.file;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MapDescriptionYamlGeneratorTest {

  private File exampleMapFolder;
  private Path propertiesFilePath;

  @BeforeEach
  void setup() throws Exception {
    exampleMapFolder =
        new File(
            MapDescriptionYamlGeneratorTest.class
                .getClassLoader()
                .getResource("map_description_yml_generator/example-map")
                .toURI());

    propertiesFilePath =
        exampleMapFolder.toPath().resolveSibling(exampleMapFolder.getName() + ".properties");
  }

  // functionality under test
  private MapDescriptionYaml generateAndReadDescriptionYamlFile() {
    final File generatedFile =
        MapDescriptionYamlGenerator.generateYamlDataForMap(exampleMapFolder).orElseThrow();
    generatedFile.deleteOnExit();

    // after generation of the YAML file, read it back so we can compare written data to expected
    return MapDescriptionYamlReader.readFromMap(exampleMapFolder).orElseThrow();
  }

  @Test
  void noVersionFile() {
    propertiesFilePath.toFile().delete();

    final MapDescriptionYaml mapDescriptionYaml = generateAndReadDescriptionYamlFile();

    assertThat(
        "Without a properties file present, map version should default to zero",
        mapDescriptionYaml.getMapVersion(),
        is(0));
  }

  @Test
  void versionFileWithNoVersionValueInIt() throws Exception {
    Files.writeString(propertiesFilePath, "mapVersion=");

    final MapDescriptionYaml mapDescriptionYaml = generateAndReadDescriptionYamlFile();

    assertThat(
        "If the version file is invalid, or missing a version value, "
            + "then map versions should default to zero.",
        mapDescriptionYaml.getMapVersion(),
        is(0));
  }

  /** Verify data generated using a minimal map with properties file and two XML files. */
  @ParameterizedTest
  @ValueSource(strings = {"2", "2.0", "2.0.0"})
  void verifyGeneration(final String versionValue) throws Exception {
    Files.writeString(propertiesFilePath, "mapVersion=" + versionValue);

    final MapDescriptionYaml mapDescriptionYaml = generateAndReadDescriptionYamlFile();

    assertThat(mapDescriptionYaml.getMapName(), is("example-map"));
    assertThat(mapDescriptionYaml.getMapVersion(), is(versionValue.isBlank() ? 0 : 2));
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
            .anyMatch(g -> g.getXmlPath().equals("games/game.xml")),
        is(true));

    assertThat(
        mapDescriptionYaml.getMapGameList().stream()
            .anyMatch(g -> g.getXmlPath().equals("games/game2.xml")),
        is(true));
  }
}
