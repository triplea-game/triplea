package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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
public class DownloadedMapsListing {
  private final Collection<DownloadedMap> downloadedMaps;

  private DownloadedMapsListing() {
    this(readMapYamlsAndGenerateMissingMapYamls());
  }

  /**
   * Reads the downloaded maps folder contents, parses those contents to find available games, and
   * returns the list of available games found.
   */
  public static synchronized DownloadedMapsListing parseMapFiles() {
    return new DownloadedMapsListing();
  }

  private static Collection<DownloadedMap> readMapYamlsAndGenerateMissingMapYamls() {
    // loop over all maps, find and parse a 'map.yml' file, if not found attempt to generate it
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(File::isDirectory)
        .map(
            mapFolder ->
                MapDescriptionYaml.fromMap(mapFolder)
                    .or(() -> MapDescriptionYaml.generateForMap(mapFolder)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(DownloadedMap::new)
        .collect(Collectors.toList());
  }

  /** Returns the list of all installed game names. */
  public List<String> getSortedGameList() {
    return downloadedMaps.stream()
        .map(DownloadedMap::getGameNames)
        .flatMap(Collection::stream)
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * Checks if a given map by name is installed, returns true if so. Map name matching is done
   * case-insensitive with spaces, dashes and underscores ignored.
   */
  public boolean isMapInstalled(final String mapName) {
    final String nameToMatch = normalizeName(mapName);
    return downloadedMaps.stream()
        .map(DownloadedMap::getMapName)
        .map(DownloadedMapsListing::normalizeName)
        .anyMatch(nameToMatch::equalsIgnoreCase);
  }

  /**
   * Returns the path to the file associated with the specified game. Returns empty if there is no
   * game matching the given name.
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   * @return The full path to the game file; or {@code empty} if the game is not available.
   */
  public Optional<Path> findGameXmlPathByGameName(final String gameName) {
    return downloadedMaps.stream()
        .filter(downloadedMap -> downloadedMap.getGameNames().contains(gameName))
        .findAny()
        .flatMap(downloadedMap -> downloadedMap.getGameXmlFilePath(gameName));
  }

  public boolean hasGame(final String gameName) {
    return findGameXmlPathByGameName(gameName).isPresent();
  }

  public Integer getMapVersionByName(final String mapName) {
    return downloadedMaps.stream()
        .filter(d -> d.getMapName().equals(mapName))
        .findAny()
        .map(DownloadedMap::getMapVersion)
        .orElse(0);
  }

  /**
   * Finds the 'root' of a map folder containing map content files. This will typically be a folder
   * called something like "downloadedMaps/mapName/map". Returns empty if no map with the given name
   * is found.
   */
  public Optional<Path> findContentRootForMapName(final String mapName) {
    final String nameToFind = normalizeName(mapName);
    return downloadedMaps.stream()
        .filter(d -> nameToFind.equals(normalizeName(d.getMapName())))
        .findAny()
        .flatMap(DownloadedMap::findContentRoot);
  }

  private static String normalizeName(final String mapName) {
    return mapName
        .toLowerCase() //
        .replaceAll("_", "")
        .replaceAll(" ", "")
        .replaceAll("-", "");
  }

  /**
   * Finds the map folder storing a given map by name. The map folder is assumed to be the parent
   * directory of the 'map.yml' file describing that map.
   */
  public Optional<File> findMapFolderByName(final String mapName) {
    return findContentRootForMapName(mapName).map(Path::getParent).map(Path::toFile);
  }

  /**
   * Creates and returns the list of 'game-chooser-entries' that can be presented to a user for game
   * selection.
   */
  public Collection<DefaultGameChooserEntry> createGameChooserEntries() {
    return downloadedMaps.stream()
        .map(DownloadedMapsListing::convertDownloadedMapToChooserEntries)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(DefaultGameChooserEntry::getGameName))
        .collect(Collectors.toList());
  }

  private static Collection<DefaultGameChooserEntry> convertDownloadedMapToChooserEntries(
      final DownloadedMap downloadedMap) {

    return downloadedMap.getGameNames().stream()
        .map(
            gameName ->
                DefaultGameChooserEntry.builder()
                    .downloadedMap(downloadedMap)
                    .gameName(gameName)
                    .build())
        .collect(Collectors.toList());
  }
}
