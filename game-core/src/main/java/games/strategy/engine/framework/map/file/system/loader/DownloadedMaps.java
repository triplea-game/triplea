package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.map.download.DownloadFileProperties;
import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.io.FileUtils;

/**
 * Data structure for the list of available games, games that a player has downloaded or installed
 * onto their hard drive.
 */
@AllArgsConstructor
public class DownloadedMaps {
  private final Collection<DefaultGameChooserEntry> availableGames;

  /**
   * Reads the downloaded maps folder contents, parses those contents to find available games, and
   * returns the list of available games found.
   */
  public static synchronized DownloadedMaps parseMapFiles() {
    return AvailableGamesFileSystemReader.parseMapFiles();
  }

  /**
   * Finds the 'root' of a map folder containing map content files. This will typically be a folder
   * called something like "downloadedMaps/mapName/map". Returns empty if no map with the given name
   * is found.
   */
  public static Optional<File> findPathToMapFolder(final String mapName) {
    return FileSystemMapFinder.getPath(mapName);
  }

  public List<String> getSortedGameList() {
    return availableGames.stream()
        .map(DefaultGameChooserEntry::getGameName)
        .sorted(Comparator.comparing(String::toUpperCase))
        .collect(Collectors.toList());
  }

  public List<DefaultGameChooserEntry> getSortedGameEntries() {
    return availableGames.stream().sorted().collect(Collectors.toList());
  }

  public boolean hasGame(final String gameName) {
    return findGameUriByName(gameName).isPresent();
  }

  /**
   * Returns the path to the file associated with the specified game. Returns empty if there is no
   * game matching the given name.
   *
   * <p>The "path" is actually a URI in string form.
   *
   * @param gameName The name of the game whose file path is to be retrieved; may be {@code null}.
   * @return The path to the game file; or {@code null} if the game is not available.
   */
  public Optional<URI> findGameUriByName(final String gameName) {
    return availableGames.stream()
        .filter(entry -> entry.getGameName().equals(gameName))
        .findAny()
        .map(DefaultGameChooserEntry::getUri);
  }

  /**
   * For a given a map name (fuzzy matched), if the map is installed, will return the map version
   * installed othewrise returns an empty. Fuzzy matching means we will do name normalization and
   * replace spaces with underscores, convert to lower case, etc.
   */
  public static Optional<Integer> getMapVersionByName(final String mapName) {
    final String normalizedMapName = normalizeName(mapName);

    // see if we have a map folder that matches the target name
    // if so, check for a .properties file
    // if that exists, then return the map version value from the properties file
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(file -> normalizeName(file.getName()).equals(normalizedMapName))
        .findAny()
        .map(
            file ->
                ClientFileSystemHelper.getUserMapsFolder()
                    .toPath()
                    .resolve(file.getName() + ".properties")
                    .toFile())
        .filter(File::exists)
        .flatMap(DownloadedMaps::readVersionFromPropertyFile);
  }

  private static Optional<Integer> readVersionFromPropertyFile(final File propertyFile) {
    return DownloadFileProperties.loadForZipPropertyFile(propertyFile).getVersion();
  }

  /**
   * Returns a normalized version of the input. Trims off a '.properties' suffix if present,
   * converts to lower case and replaces all spaces with underscores.
   */
  private static String normalizeName(final String inputName) {
    String normalizedName = inputName;
    if (inputName.endsWith(".zip.properties")) {
      normalizedName = inputName.substring(0, inputName.indexOf(".zip.properties"));
    }
    if (normalizedName.endsWith("-master")) {
      normalizedName = inputName.substring(0, inputName.indexOf("-master"));
    }

    normalizedName = normalizedName.replaceAll(" ", "_");
    normalizedName = normalizedName.toLowerCase();

    return normalizedName;
  }
}
