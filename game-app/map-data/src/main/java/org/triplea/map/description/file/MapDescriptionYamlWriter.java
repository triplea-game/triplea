package org.triplea.map.description.file;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.yaml.YamlWriter;

/**
 * Class responsible for converting a {@code MapDescriptionYaml} to a Yaml formatted string and
 * writing that string to file.
 */
@UtilityClass
@Slf4j
class MapDescriptionYamlWriter {
  static Optional<Path> writeYmlPojoToFile(final MapDescriptionYaml mapDescriptionYaml) {

    final String yamlString = toYamlString(mapDescriptionYaml);
    final Path mapYmlTargetPath = Path.of(mapDescriptionYaml.getYamlFileLocation());

    try {
      Files.writeString(mapYmlTargetPath, yamlString);
      log.info("Wrote map yaml to file: {}", mapYmlTargetPath.toAbsolutePath());
      return Optional.of(mapYmlTargetPath);
    } catch (final IOException e) {
      log.error(
          "Failed to write map.yml file to: {}, {}",
          mapYmlTargetPath.toAbsolutePath(),
          e.getMessage(),
          e);
      return Optional.empty();
    }
  }

  @VisibleForTesting
  String toYamlString(final MapDescriptionYaml mapDescriptionYaml) {
    final Map<String, Object> data = new HashMap<>();
    data.put(MapDescriptionYaml.YamlKeys.MAP_NAME, mapDescriptionYaml.getMapName());
    // generate game list
    data.put(
        MapDescriptionYaml.YamlKeys.GAMES_LIST,
        mapDescriptionYaml.getMapGameList().stream()
            .map(MapDescriptionYamlWriter::mapGameToYamlDataMap)
            .collect(Collectors.toList()));

    return YamlWriter.writeToString(data);
  }

  private static Map<String, Object> mapGameToYamlDataMap(final MapDescriptionYaml.MapGame game) {
    final Map<String, Object> gameYamlData = new HashMap<>();
    gameYamlData.put(MapDescriptionYaml.YamlKeys.GAME_NAME, game.getGameName());
    gameYamlData.put(MapDescriptionYaml.YamlKeys.FILE_NAME, game.getXmlFileName());
    return gameYamlData;
  }
}
