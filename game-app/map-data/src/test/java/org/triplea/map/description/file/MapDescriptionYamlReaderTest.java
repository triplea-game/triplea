package org.triplea.map.description.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class MapDescriptionYamlReaderTest {

  @Test
  @DisplayName("Verify able to read a valid map.yml file from an example map folder")
  void readFromFolder() throws Exception {
    final URI sampleFolderUri =
        MapDescriptionYamlReaderTest.class
            .getClassLoader()
            .getResource("map_description_yml_parsing/example_directory")
            .toURI();

    final MapDescriptionYaml mapDescriptionYaml =
        MapDescriptionYamlReader.readFromMap(Path.of(sampleFolderUri)).orElseThrow();

    assertThat(mapDescriptionYaml.isValid(Path.of(""))).isTrue();
  }

  @Test
  void shouldReturnEmptyIfYmlMissingFromFolder() throws Exception {
    final URI sampleFolderUri =
        MapDescriptionYamlReaderTest.class
            .getClassLoader()
            .getResource("map_description_yml_parsing/example_missing_yml_directory")
            .toURI();

    final Optional<MapDescriptionYaml> mapDescription =
        MapDescriptionYamlReader.readFromMap(Path.of(sampleFolderUri));

    assertThat(mapDescription).isEmpty();
  }

  @Test
  @DisplayName("Verify parsing a specific map.yml file and that parsed data is correct")
  void readSampleMapDescription() throws Exception {
    try (InputStream stream =
        MapDescriptionYamlReaderTest.class
            .getClassLoader()
            .getResourceAsStream("map_description_yml_parsing/sample_map_description.yml")) {

      final MapDescriptionYaml mapDescriptionYaml =
          MapDescriptionYamlReader.parse(
                  Path.of("map_description_yml_parsing/sample_map_description.yml"), stream)
              .orElseThrow();

      assertThat(mapDescriptionYaml.getMapName()).isEqualTo("MapName");
      assertThat(mapDescriptionYaml.getMapGameList()).hasSize(2);
      assertThat(mapDescriptionYaml.getMapGameList().get(0).getGameName()).isEqualTo("GameName0");
      assertThat(mapDescriptionYaml.getMapGameList().get(0).getXmlFileName())
          .isEqualTo("XmlGameFile0.xml");
      assertThat(mapDescriptionYaml.getMapGameList().get(1).getGameName()).isEqualTo("GameName1");
      assertThat(mapDescriptionYaml.getMapGameList().get(1).getXmlFileName())
          .isEqualTo("XmlGameFile1.xml");
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "map_description_yml_parsing/invalid_map_description.yml",
        "map_description_yml_parsing/empty_map_description.yml",
        "map_description_yml_parsing/empty_game_list_description.yml"
      })
  void invalidYamls(final String inputFile) throws Exception {
    try (InputStream stream =
        MapDescriptionYamlReaderTest.class.getClassLoader().getResourceAsStream(inputFile)) {

      final Optional<MapDescriptionYaml> result =
          MapDescriptionYamlReader.parse(Path.of(inputFile), stream);

      assertThat(result).isEmpty();
    }
  }
}
