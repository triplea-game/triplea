package org.triplea.map.description.file;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.generic.xml.reader.XmlMapper;
import org.triplea.generic.xml.reader.exceptions.XmlParsingException;
import org.triplea.io.FileUtils;
import org.triplea.map.data.elements.Game;
import org.triplea.map.data.elements.Info;

/**
 * Builds a MapDescriptionYaml file from scratch by reading an existing map and corresponding
 * version file. This item is in place to support legacy maps that do not have a map.yml file, for
 * such a case we can generate it.
 *
 * <p>To generate the data file, we need to parse the map to find XMLs, find the game name in that
 * XML, and also we need to parse a map properties file to find the downloaded map version.
 */
@Slf4j
@UtilityClass
class MapDescriptionYamlGenerator {

  /**
   * Reads a map and generates a 'yml' file within that map. An empty is returned if the folder is
   * not actually a map or if any other error occurs.
   *
   * <p>The map version is read from a '.properties' file that is expected to be a sibling of the
   * map folder. If we successfully generate a 'map.yml' file then the '.properties' file will be
   * removed.
   *
   * @param mapFolder An existing map that (presumably) does not have a 'map.yml' file where we will
   *     be generated a map.yml descriptor file. The map.yml file will be created at the map's
   *     content root, where the map data files are located.
   */
  static Optional<File> generateYamlDataForMap(final File mapFolder) {
    Preconditions.checkArgument(mapFolder.exists());
    Preconditions.checkArgument(mapFolder.isDirectory());

    final String mapName = mapFolder.getName();

    final File propsFile =
        mapFolder.toPath().resolveSibling(mapFolder.getName() + ".properties").toFile();
    final int downloadVersion = propsFile.exists() ? readDownloadVersion(propsFile) : 0;

    final List<MapDescriptionYaml.MapGame> games = readGameInformationFromXmls(mapFolder.toPath());
    if (games.isEmpty()) {
      return Optional.empty();
    }

    final Path mapYmlTargetFileLocation =
        mapFolder.toPath().resolve(MapDescriptionYaml.MAP_YAML_FILE_NAME);

    final MapDescriptionYaml mapDescriptionYaml =
        MapDescriptionYaml.builder()
            .yamlFileLocation(mapYmlTargetFileLocation.toUri())
            .mapName(mapName)
            .mapVersion(downloadVersion)
            .mapGameList(games)
            .build();

    if (!mapDescriptionYaml.isValid(mapFolder)) {
      return Optional.empty();
    }

    final Optional<File> writtenYmlFile =
        MapDescriptionYamlWriter.writeYmlPojoToFile(mapDescriptionYaml);

    if (writtenYmlFile.isPresent() && propsFile.exists()) {
      // clean up the properties file, it is no longer needed
      propsFile.delete();
    }
    return writtenYmlFile;
  }

  /** Parses an XML converting the XML tag information into POJO. */
  private static Optional<Game> parseXmlTags(final File xmlFile) {
    try (InputStream inputStream = new FileInputStream(xmlFile)) {
      return Optional.of(new XmlMapper(inputStream).mapXmlToObject(Game.class));
    } catch (final XmlParsingException | IOException e) {
      log.info("Unable to parse XML file: " + xmlFile.getAbsolutePath(), e);
      return Optional.empty();
    }
  }

  /** Reads a properties file for the map version or zero if it cannot be read. */
  private static int readDownloadVersion(final File propsFile) {
    final Properties props = new Properties();
    try (InputStream fis = new FileInputStream(propsFile)) {
      props.load(fis);
    } catch (final IllegalArgumentException | IOException e) {
      log.error(
          "Failed to read property file: " + propsFile.getAbsolutePath() + ", " + e.getMessage(),
          e);
      return 0;
    }

    // mapVersion will likely be in a '2.0.0' format. We expect only the leading digit to ever
    // be non-zero. This is because we get an integer from the download description, but this
    // was written as 'Version' object which appends trailing zeros.
    return Optional.ofNullable(props.getProperty("mapVersion"))
        .flatMap(
            version ->
                Arrays.stream(version.split("\\.")) //
                    .findFirst()
                    .map(Ints::tryParse))
        .orElse(0);
  }

  /**
   * Parses all XMLs for game information and returns a POJO representation. This is the game name
   * and a relativized path to the Game XML from relative to the maps content root.
   */
  private static List<MapDescriptionYaml.MapGame> readGameInformationFromXmls(
      final Path mapFolder) {
    // We expect the needed search depth to be 2, eg: 'map/contentRoot/games/game.xml'
    // We add a bit extra for flexibility.
    final int maxXmlSearchDepth = 4;
    return FileUtils.findXmlFiles(mapFolder.toFile(), maxXmlSearchDepth).stream()
        .map(
            xmlFile ->
                readGameNameFromXml(xmlFile)
                    .map(
                        gameName ->
                            MapDescriptionYaml.MapGame.builder()
                                .gameName(gameName)
                                .xmlPath(mapFolder.relativize(xmlFile.toPath()).toString())
                                .build())
                    .orElse(null))
        .collect(Collectors.toList());
  }

  private static Optional<String> readGameNameFromXml(final File xmlFile) {
    return parseXmlTags(xmlFile)
        .map(Game::getInfo)
        .map(Info::getName)
        .filter(Predicate.not(String::isBlank));
  }
}
