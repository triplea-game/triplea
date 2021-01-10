package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.io.File;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

/**
 * Data structure for the list of available games, games that a player has downloaded or installed
 * onto their hard drive.
 */
@AllArgsConstructor
public class DownloadedMaps {
  private final Set<DefaultGameChooserEntry> availableGames;

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
}
