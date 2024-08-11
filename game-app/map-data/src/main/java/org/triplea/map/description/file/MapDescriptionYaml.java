package org.triplea.map.description.file;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.FileUtils;

/**
 * POJO data structure representing the contents of a map.yml file along with operations to read,
 * write and generate a map.yml file. The data describes which game XML files are in a map, their
 * name, path, and the name and download version of the map.
 *
 * <p>Example YAML structure:
 *
 * <pre>
 *   map_name: [string]
 *   games:
 *   - name: [string]
 *     xml_path: [string]
 * </pre>
 */
@Getter
@ToString
@Slf4j
@Builder
@AllArgsConstructor
@EqualsAndHashCode
public class MapDescriptionYaml {

  @NonNls public static final String MAP_YAML_FILE_NAME = "map.yml";

  private static final int MAX_GAME_NAME_LENGTH = 70;
  private static final int MAX_MAP_NAME_LENGTH = 70;

  /** The location on disk where we read this 'map description yaml' data. */
  @Nonnull private final Path yamlFileLocation;

  @Nonnull private final String mapName;

  @Singular(value = "game")
  @Nonnull
  private final List<MapGame> mapGameList;

  interface YamlKeys {
    String MAP_NAME = "map_name";
    String GAMES_LIST = "games";
    String GAME_NAME = "game_name";
    String FILE_NAME = "file_name";
  }

  /**
   * Represents a single list node describing a game contained in a map. Example structure:
   *
   * <pre>
   * - game_name: [string]
   *   file_name: [string]
   * </pre>
   */
  @Getter
  @ToString
  @Builder
  @AllArgsConstructor
  @EqualsAndHashCode
  public static class MapGame {
    private final String gameName;

    /** Path of the XML file relative to the location of map.yml file. */
    private final String xmlFileName;
  }

  /** Dumps (writes) the current data represented in this object into a YAML formatted string. */
  public String toYamlString() {
    return MapDescriptionYamlWriter.toYamlString(this);
  }

  public static boolean mapHasYamlDescriptor(final Path mapFolder) {
    return Files.exists(mapFolder.resolve(MAP_YAML_FILE_NAME));
  }

  public static Optional<MapDescriptionYaml> fromMap(final Path mapFolder) {
    return MapDescriptionYamlReader.readFromMap(mapFolder);
  }

  public static Optional<MapDescriptionYaml> fromFile(final Path mapDescriptionYamlFile) {
    Preconditions.checkArgument(
        mapDescriptionYamlFile.getFileName().toString().equals(MAP_YAML_FILE_NAME),
        mapDescriptionYamlFile.toAbsolutePath());

    return MapDescriptionYamlReader.readYmlFile(mapDescriptionYamlFile);
  }

  public static Optional<MapDescriptionYaml> generateForMap(final Path mapFolder) {
    if (MapDescriptionYamlGenerator.generateYamlDataForMap(mapFolder).isEmpty()) {
      // failed to generate
      return Optional.empty();
    }
    return fromMap(mapFolder);
  }

  boolean isValid(final Path sourceFile) {
    final Collection<String> validationErrors = new ArrayList<>();

    if (mapName.isBlank()) {
      validationErrors.add(YamlKeys.MAP_NAME + " attribute empty or not found");
    } else if (mapName.length() > MAX_MAP_NAME_LENGTH) {
      validationErrors.add(mapName + " is too long, exceeds max length of: " + MAX_MAP_NAME_LENGTH);
    }

    if (mapGameList.isEmpty()) {
      validationErrors.add(YamlKeys.GAMES_LIST + " is empty");
    } else {
      if (mapGameList.stream().anyMatch(game -> Strings.nullToEmpty(game.gameName).isBlank())) {
        validationErrors.add(
            YamlKeys.GAME_NAME
                + " attribute empty, misspelled or missing in "
                + YamlKeys.GAMES_LIST
                + " list");
      }

      if (mapGameList.stream().anyMatch(game -> Strings.nullToEmpty(game.xmlFileName).isBlank())) {
        validationErrors.add(
            YamlKeys.FILE_NAME
                + " attribute empty, misspelled or missing in "
                + YamlKeys.GAMES_LIST
                + " list");
      } else if (mapGameList.stream()
          .anyMatch(game -> !Strings.nullToEmpty(game.xmlFileName).endsWith(".xml"))) {
        validationErrors.add(
            YamlKeys.FILE_NAME + " value must be name of an xml file (end with .xml");
      }
    }

    if (!validationErrors.isEmpty()) {
      log.info(
          "Warning: invalid map found installed: {}, errors: {}",
          sourceFile.toAbsolutePath(),
          validationErrors);
    }

    return validationErrors.isEmpty();
  }

  /**
   * Given the name of a game, returns the path to the corresponding game XML file. This works by
   * looking up the game file name in the 'map.yml' file, then we search for a 'games' folder, and
   * then underneath that folder we search for a matching file name.
   */
  public Optional<Path> getGameXmlPathByGameName(final String gameName) {
    final Optional<String> xmlFileName = findFileNameForGame(gameName);
    final Optional<Path> gamesFolder = findGamesFolder();

    if (xmlFileName.isEmpty() || gamesFolder.isEmpty()) {
      return Optional.empty();
    } else {
      return searchForGameFile(gamesFolder.get(), xmlFileName.get());
    }
  }

  /**
   * Given an XML file, does something of a reverse lookup in the map.yml file to find the
   * corresponding game name. The match is based on the file name.
   *
   * @throws IllegalStateException thrown if no entry exists in this map.yml file with a game whose
   *     file name matches the input file name.
   */
  public String findGameNameFromXmlFileName(final Path xmlFile) {
    // Find map game entry whose xml file name matches input file name.
    // Once found, return the corresponding game name.
    return mapGameList.stream()
        .filter(game -> game.getXmlFileName().equals(xmlFile.getFileName().toString()))
        .findAny()
        .map(MapGame::getGameName)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "map.yml file at: "
                        + yamlFileLocation
                        + ", did not contain an entry"
                        + "for "
                        + xmlFile.toAbsolutePath()));
  }

  /** Lookup game XML file name in map.yml by game name. */
  private Optional<String> findFileNameForGame(final String gameName) {
    final Optional<String> fileName =
        mapGameList.stream()
            .filter(map -> map.getGameName().equalsIgnoreCase(gameName))
            .findAny()
            .map(MapGame::getXmlFileName);

    if (fileName.isEmpty()) {
      log.warn(
          "Failed to find game name {} in map.yml file, map.yml had the following entries: {}",
          gameName,
          mapGameList.stream().map(MapGame::getGameName).sorted().collect(Collectors.toList()));
    }
    return fileName;
  }

  /** Find 'games' folder starting from map.yml parent folder. */
  private Optional<Path> findGamesFolder() {
    final Path mapFolder = yamlFileLocation.getParent();
    final Optional<Path> gamesFolder = FileUtils.findClosestToRoot(mapFolder, 5, "games");

    if (gamesFolder.isEmpty()) {
      log.warn("No 'games' folder found under location: {}", mapFolder.toAbsolutePath());
    }
    return gamesFolder;
  }

  /** Search 'games' folder for a game-xml-file. */
  private Optional<Path> searchForGameFile(final Path gamesFolder, final String xmlFileName) {
    final Optional<Path> gameFile = FileUtils.findClosestToRoot(gamesFolder, 3, xmlFileName);
    if (gameFile.isEmpty()) {
      log.warn(
          "Failed to find game file: {}, within directory tree rooted at: {}",
          xmlFileName,
          gamesFolder.toAbsolutePath());
    }
    return gameFile;
  }
}
