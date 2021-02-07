package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.triplea.ui.mapdata.MapData;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.io.FileUtils;
import org.triplea.map.description.file.MapDescriptionYaml;

/**
 * Data structure for the list of available games, games that a player has downloaded or installed
 * onto their hard drive.
 */
@AllArgsConstructor
public class DownloadedMaps {
  // The set of all 'map.yml' files found on disk
  private final Collection<MapDescriptionYaml> mapDescriptionYamls;

  private DownloadedMaps() {
    this(readMapYamlsAndGenerateMissingMapYamls());
  }

  /**
   * Reads the downloaded maps folder contents, parses those contents to find available games, and
   * returns the list of available games found.
   */
  public static synchronized DownloadedMaps parseMapFiles() {
    return new DownloadedMaps();
  }

  private static Collection<MapDescriptionYaml> readMapYamlsAndGenerateMissingMapYamls() {
    // loop over all maps, find and parse a 'map.yml' file, if not found attempt to generate it
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(File::isDirectory)
        .map(
            mapFolder ->
                MapDescriptionYaml.fromMap(mapFolder)
                    .or(() -> MapDescriptionYaml.generateForMap(mapFolder)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  /** Returns the list of all installed game names. */
  public List<String> getSortedGameList() {
    return getGameNamesToGameLocations().keySet().stream().sorted().collect(Collectors.toList());
  }

  /** Returns the set of all downloaded game names mapped to their XML file location. */
  public Map<String, Path> getGameNamesToGameLocations() {
    final Map<String, Path> gameNamesToPaths = new HashMap<>();
    for (final MapDescriptionYaml mapDescriptionYaml : mapDescriptionYamls) {
      mapDescriptionYaml.getMapGameList().stream()
          .map(MapDescriptionYaml.MapGame::getGameName)
          .forEach(
              gameName -> {
                final Path xmlFilePath =
                    mapDescriptionYaml.getGameXmlPathByGameName(gameName).orElseThrow();
                gameNamesToPaths.put(gameName, xmlFilePath);
              });
    }
    return gameNamesToPaths;
  }

  /**
   * Returns the path to the file associated with the specified game. Returns empty if there is no
   * game matching the given name.
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   * @return The full path to the game file; or {@code empty} if the game is not available.
   */
  public Optional<Path> findGameXmlPathByGameName(final String gameName) {
    return mapDescriptionYamls.stream()
        .map(mapDescriptionYaml -> mapDescriptionYaml.getGameXmlPathByGameName(gameName))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findAny();
  }

  public boolean hasGame(final String gameName) {
    return findGameXmlPathByGameName(gameName).isPresent();
  }

  public Optional<Integer> getMapVersionByName(final String mapName) {
    return mapDescriptionYamls.stream()
        .filter(yaml -> normalizeName(yaml.getMapName()).equals(normalizeName(mapName)))
        .findAny()
        .map(MapDescriptionYaml::getMapVersion);
  }

  /**
   * Finds the 'root' of a map folder containing map content files. This will typically be a folder
   * called something like "downloadedMaps/mapName/map". Returns empty if no map with the given name
   * is found.
   */
  public static Optional<File> findContentRootForMapName(final String mapName) {
    // Find a 'map.yml' with the given map name.
    // Find the parent folder for that 'map.yml'
    // Search that location and underneath for a 'polygons' file.
    // If found, that location is our content root.
    final Path mapYamlParentFolder =
        new DownloadedMaps().findMapYamlFileForMapName(mapName).map(Path::getParent).orElse(null);
    if (mapYamlParentFolder == null) {
      return Optional.empty();
    }

    return FileUtils.findFile(mapYamlParentFolder, 3, MapData.POLYGON_FILE)
        .map(File::toPath)
        .map(Path::getParent)
        .map(Path::toFile);
  }

  private Optional<Path> findMapYamlFileForMapName(final String mapName) {
    return mapDescriptionYamls.stream()
        .filter(m -> normalizeName(m.getMapName()).equalsIgnoreCase(normalizeName(mapName)))
        .findAny()
        .map(MapDescriptionYaml::getYamlFileLocation)
        .map(Path::of);
  }

  private static String normalizeName(final String mapName) {
    return mapName
        .toLowerCase() //
        .replaceAll("_", "")
        .replaceAll(" ", "")
        .replaceAll("-", "");
  }

  public File findContentRootForMapNameOrElseThrow(final String mapName) {
    return findContentRootForMapName(mapName)
        .orElseThrow(() -> new IllegalArgumentException("Unable to find map: " + mapName));
  }

  /**
   * Finds the map folder storing a given map by name. The map folder is assumed to be the parent
   * directory of the 'map.yml' file describing that map.
   */
  public Optional<File> findMapFolderByName(final String mapName) {
    return findMapYamlFileForMapName(mapName) //
        .map(Path::getParent)
        .map(Path::toFile);
  }
}
