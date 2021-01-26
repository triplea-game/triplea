package org.triplea.map.description.file;

import com.google.common.base.Strings;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * POJO data structure representing the contents of a map.yml file along with operations to read,
 * write and generate a map.yml file. The data describes which game XML files are in a map, their
 * name, path, and the name and download version of the map.
 *
 * <p>Example YAML structure:
 *
 * <pre>
 *   map_name: [string]
 *   version: [number]
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

  public static final String MAP_YAML_FILE_NAME = "map.yml";

  private static final int MAX_GAME_NAME_LENGTH = 64;
  private static final int MAX_MAP_NAME_LENGTH = 64;


  @Nonnull private final URI yamlFileLocation;
  @Nonnull private final String mapName;
  @Nonnull private final Integer mapVersion;
  @Nonnull private final List<MapGame> mapGameList;

  interface YamlKeys {
    String MAP_NAME = "map_name";
    String VERSION = "version";
    String GAMES_LIST = "games";
    String GAME_NAME = "name";
    String XML_PATH = "xml_path";
  }

  /**
   * Represents a single list node describing a game contained in a map. Example structure:
   *
   * <pre>
   * - name: [string]
   *   xml_path: [string]
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
    private final String xmlPath;
  }

  /** Dumps (writes) the current data represented in this object into a YAML formatted string. */
  public String toYamlString() {
    return MapDescriptionYamlWriter.toYamlString(this);
  }

  public static Optional<MapDescriptionYaml> fromMap(final File mapFolder) {
    return MapDescriptionYamlReader.readFromMap(mapFolder);
  }

  public static Optional<MapDescriptionYaml> generateForMap(final File mapFolder) {
    if (MapDescriptionYamlGenerator.generateYamlDataForMap(mapFolder).isEmpty()) {
      // failed to generate
      return Optional.empty();
    }
    return fromMap(mapFolder);
  }

  boolean isValid(final File sourceFile) {
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
        validationErrors.add(YamlKeys.GAME_NAME + " attribute empty or missing");
      }

      if (mapGameList.stream().anyMatch(game -> Strings.nullToEmpty(game.xmlPath).isBlank())) {
        validationErrors.add(YamlKeys.XML_PATH + " attribute empty or missing");
      } else if (mapGameList.stream()
          .anyMatch(game -> !Strings.nullToEmpty(game.xmlPath).endsWith(".xml"))) {
        validationErrors.add(
            YamlKeys.XML_PATH + " value must be a path to an xml file (end with .xml");
      }
    }

    if (!validationErrors.isEmpty()) {
      log.warn("Error found in: {}, errors: {}", sourceFile.getAbsolutePath(), validationErrors);
    }

    return validationErrors.isEmpty();
  }

  public Optional<Path> getGameXmlPathByGameName(final String gameName) {
    return mapGameList.stream()
        .filter(map -> map.getGameName().equals(gameName))
        .findAny()
        .map(MapGame::getXmlPath)
        .map(path -> new File(yamlFileLocation).toPath().getParent().resolve(path));
  }
}
