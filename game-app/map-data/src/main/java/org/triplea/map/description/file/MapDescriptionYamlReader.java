package org.triplea.map.description.file;

import static org.triplea.map.description.file.MapDescriptionYaml.YamlKeys.GAMES_LIST;
import static org.triplea.map.description.file.MapDescriptionYaml.YamlKeys.MAP_NAME;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.FileUtils;
import org.triplea.map.description.file.MapDescriptionYaml.YamlKeys;
import org.triplea.yaml.YamlReader;
import org.triplea.yaml.YamlReader.InvalidYamlFormatException;

/** Reads a 'map.yml' file from disk and returns a POJO representation. */
@Slf4j
@UtilityClass
class MapDescriptionYamlReader {

  static Optional<MapDescriptionYaml> readFromMap(final Path mapFolder) {
    Preconditions.checkArgument(Files.exists(mapFolder));

    return findMapYmlFile(mapFolder) //
        .flatMap(MapDescriptionYamlReader::readYmlFile);
  }

  private static Optional<Path> findMapYmlFile(final Path mapFolder) {
    /*
     * We expect a map.yml file at depth of 1, eg: "map_folder/map/map.yml", but we can also find a
     * 'map.yml' file at depth 2, eg: 'map_folder-master/map_folder/map/map.yml'.
     */
    final int maxMapYmlSearchDepth = 2;
    return FileUtils.findClosestToRoot(
        mapFolder, maxMapYmlSearchDepth, MapDescriptionYaml.MAP_YAML_FILE_NAME);
  }

  /** Factory method, finds the map.yml file in a folder and reads it. */
  static Optional<MapDescriptionYaml> readYmlFile(final Path ymlFile) {
    Preconditions.checkArgument(Files.isReadable(ymlFile));
    Preconditions.checkArgument(
        ymlFile.getFileName().toString().equals(MapDescriptionYaml.MAP_YAML_FILE_NAME));

    return FileUtils.openInputStream(ymlFile, inputStream -> parse(ymlFile, inputStream));
  }

  static Optional<MapDescriptionYaml> parse(final Path ymlFile, final InputStream inputStream) {
    Preconditions.checkNotNull(inputStream);

    try {
      final Map<String, Object> yamlData = YamlReader.readMap(inputStream);
      final MapDescriptionYaml mapDescriptionYaml =
          MapDescriptionYaml.builder()
              .yamlFileLocation(ymlFile)
              .mapName(Strings.nullToEmpty((String) yamlData.get(MAP_NAME)))
              .mapGameList(parseGameList(yamlData))
              .build();

      if (!mapDescriptionYaml.isValid(ymlFile)) {
        log.warn(
            "Invalid map description YML (map.yml) file detected: {}\n"
                + "Check the file carefully and correct any mistakes.\n"
                + "If this is a map you downloaded, please contact TripleA.\n"
                + "Data parsed:\n"
                + "{}",
            ymlFile.toAbsolutePath(),
            mapDescriptionYaml);
        return Optional.empty();
      }
      return Optional.of(mapDescriptionYaml);
    } catch (final ClassCastException | InvalidYamlFormatException e) {
      log.warn(
          "Invalid map description YML (map.yml) file detected: {}.\n"
              + "If this is a map you downloaded, please contact TripleA.\n"
              + "{}",
          ymlFile.toAbsolutePath(),
          e.getMessage(),
          e);
      return Optional.empty();
    }
  }

  @SuppressWarnings("unchecked")
  private static List<MapDescriptionYaml.MapGame> parseGameList(
      final Map<String, Object> yamlData) {
    final List<Map<String, String>> gameList = (List<Map<String, String>>) yamlData.get(GAMES_LIST);

    return gameList == null
        ? List.of()
        : gameList.stream()
            .map(MapDescriptionYamlReader::parseMapGame)
            .collect(Collectors.toList());
  }

  private static MapDescriptionYaml.MapGame parseMapGame(final Map<String, String> yamlData) {
    return MapDescriptionYaml.MapGame.builder()
        .gameName(Strings.nullToEmpty(yamlData.get(YamlKeys.GAME_NAME)))
        .xmlFileName(Strings.nullToEmpty(yamlData.get(YamlKeys.FILE_NAME)))
        .build();
  }
}
