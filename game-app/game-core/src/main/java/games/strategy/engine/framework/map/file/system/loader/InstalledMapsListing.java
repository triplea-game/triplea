package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Singular;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.map.description.file.MapDescriptionYaml;

/**
 * Data structure for the list of available games, games that a player has downloaded or installed
 * onto their hard drive.
 */
@Builder
@AllArgsConstructor
public class InstalledMapsListing {
  @Singular private final Collection<InstalledMap> installedMaps;

  private InstalledMapsListing(Path folder) {
    this(readMapYamlsAndGenerateMissingMapYamls(folder));
  }

  /**
   * Reads the downloaded maps folder contents, parses those contents to find available games, and
   * returns the list of available games found.
   */
  public static synchronized InstalledMapsListing parseMapFiles() {
    final List<Path> mapFolders = new ArrayList<>();
    mapFolders.add(ClientFileSystemHelper.getUserMapsFolder());
    findEngineMapsFolder().ifPresent(mapFolders::add);
    return parseMapFiles(mapFolders);
  }

  public static synchronized InstalledMapsListing parseMapFiles(Path folder) {
    return parseMapFiles(List.of(folder));
  }

  static synchronized InstalledMapsListing parseMapFiles(Collection<Path> folders) {
    final Map<String, InstalledMap> mapsByName = new LinkedHashMap<>();
    folders.stream()
        .filter(Files::isDirectory)
        .flatMap(folder -> readMapYamlsAndGenerateMissingMapYamls(folder).stream())
        .forEach(map -> mapsByName.putIfAbsent(normalizeName(map.getMapName()), map));
    return new InstalledMapsListing(List.copyOf(mapsByName.values()));
  }

  private static Optional<Path> findEngineMapsFolder() {
    try {
      final Path engineMapsFolder = ClientFileSystemHelper.getRootFolder().resolve("maps");
      return Files.isDirectory(engineMapsFolder) ? Optional.of(engineMapsFolder) : Optional.empty();
    } catch (final IllegalStateException e) {
      return Optional.empty();
    }
  }

  public static Optional<Path> searchAllMapsForMapName(String mapName) {
    return parseMapFiles().findContentRootForMapName(mapName);
  }

  private static Collection<InstalledMap> readMapYamlsAndGenerateMissingMapYamls(Path folder) {
    // loop over all maps, find and parse a 'map.yml' file, if not found attempt to generate it
    return FileUtils.listFiles(folder).stream()
        .filter(Files::isDirectory)
        .map(MapDescriptionYaml::fromMap)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .map(InstalledMap::new)
        .collect(Collectors.toList());
  }

  public void deleteInstalledMapByName(final String mapName) {
    installedMaps.removeIf(installedMap -> installedMap.getMapName().equals(mapName));
  }

  /** Returns the list of all installed game names. */
  public List<String> getSortedGameList() {
    return installedMaps.stream()
        .map(InstalledMap::getGameNames)
        .flatMap(Collection::stream)
        .sorted()
        .collect(Collectors.toList());
  }

  /**
   * Checks if a given map by name is installed, returns true if so. Map name matching is done
   * case-insensitive with spaces, dashes and underscores ignored.
   */
  public boolean isMapInstalled(final String mapName) {
    return findInstalledMapByName(mapName).isPresent();
  }

  public Optional<InstalledMap> findInstalledMapByName(final String mapName) {
    final String nameToMatch = normalizeName(mapName);
    return installedMaps.stream()
        .filter(map -> normalizeName(map.getMapName()).equalsIgnoreCase(nameToMatch))
        .findAny();
  }

  /**
   * Returns the path to the file associated with the specified game. Returns empty if there is no
   * game matching the given name.
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   * @return The full path to the game file; or {@code empty} if the game is not available.
   */
  public Optional<Path> findGameXmlPathByGameName(final String gameName) {
    return installedMaps.stream()
        .filter(installedMap -> installedMap.getGameNames().contains(gameName))
        .findAny()
        .flatMap(installedMap -> installedMap.getGameXmlFilePath(gameName));
  }

  public boolean hasGame(final String gameName) {
    return findGameXmlPathByGameName(gameName).isPresent();
  }

  /**
   * Finds the 'root' of a map folder containing map content files. This will typically be a folder
   * called something like "downloadedMaps/mapName/map". Returns empty if no map with the given name
   * is found.
   */
  public Optional<Path> findContentRootForMapName(final String mapName) {
    final String nameToFind = normalizeName(mapName);
    return installedMaps.stream()
        .filter(d -> nameToFind.equals(normalizeName(d.getMapName())))
        .findAny()
        .flatMap(InstalledMap::findContentRoot);
  }

  public static String normalizeName(final String mapName) {
    return mapName.toLowerCase(Locale.ROOT).replaceAll("[_ -]", "");
  }

  /**
   * Finds the map folder storing a given map by name. The map folder is assumed to be the parent
   * directory of the 'map.yml' file describing that map.
   */
  public Optional<Path> findMapFolderByName(final String mapName) {
    return findContentRootForMapName(mapName)
        .map(
            p -> {
              Path parent = p.getParent();
              // Some maps have their content root be the map folder itself.
              if (parent.equals(ClientFileSystemHelper.getUserMapsFolder())) {
                return p;
              }
              return parent;
            });
  }

  public Optional<Path> findMapSkin(final String mapName, final String skinName) {
    return installedMaps.stream()
        .filter(d -> normalizeName(mapName).equals(normalizeName(d.getMapName())))
        .findAny()
        .flatMap(installedMap -> installedMap.findMapSkin(skinName));
  }

  /**
   * Creates and returns the list of 'game-chooser-entries' that can be presented to a user for game
   * selection.
   */
  public Collection<DefaultGameChooserEntry> createGameChooserEntries() {
    return installedMaps.stream()
        .map(InstalledMapsListing::convertDownloadedMapToChooserEntries)
        .flatMap(Collection::stream)
        .sorted(Comparator.comparing(DefaultGameChooserEntry::getGameName))
        .collect(Collectors.toList());
  }

  private static Collection<DefaultGameChooserEntry> convertDownloadedMapToChooserEntries(
      final InstalledMap installedMap) {

    return installedMap.getGameNames().stream()
        .map(
            gameName ->
                DefaultGameChooserEntry.builder()
                    .installedMap(installedMap)
                    .gameName(gameName)
                    .build())
        .collect(Collectors.toList());
  }

  /** Find any installed maps that are out of date compared to available downloads. */
  public Map<MapDownloadItem, InstalledMap> findOutOfDateMaps(
      final Collection<MapDownloadItem> downloads) {

    final Map<MapDownloadItem, InstalledMap> outOfDate = new HashMap<>();

    for (final MapDownloadItem download : downloads) {
      findInstalledMapByName(download.getMapName())
          .filter(installedMap -> installedMap.isOutOfDate(download))
          .ifPresent(installedMap -> outOfDate.put(download, installedMap));
    }
    return outOfDate;
  }
}
