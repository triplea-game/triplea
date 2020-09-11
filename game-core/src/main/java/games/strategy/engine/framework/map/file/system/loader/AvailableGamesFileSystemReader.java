package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.io.FileUtils;

/**
 * Reads from file system to find all available games. We then shallow parse each available game to
 * find the game name and we create a mapping of game name to location on disk. Values read from
 * disk are cached (can be updated by calling "{@code addNewMapToCache}).
 */
@UtilityClass
public class AvailableGamesFileSystemReader {

  private static final String ZIP_EXTENSION = ".zip";

  private static AvailableGamesList availableGamesListCache;

  public static synchronized AvailableGamesList parseMapFiles() {
    if (availableGamesListCache == null) {
      final Set<DefaultGameChooserEntry> entries = new HashSet<>();
      entries.addAll(mapXmlsGameNamesByUri(findAllZippedXmlFiles()));
      entries.addAll(mapXmlsGameNamesByUri(findAllUnZippedXmlFiles()));
      availableGamesListCache = new AvailableGamesList(entries);
    }

    return availableGamesListCache;
  }

  private Collection<DefaultGameChooserEntry> mapXmlsGameNamesByUri(
      final Collection<URI> fileList) {
    return fileList.stream()
        .map(DefaultGameChooserEntry::newDefaultGameChooserEntry)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  private static List<URI> findAllZippedXmlFiles() {
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(File::isFile)
        .filter(file -> file.getName().toLowerCase().endsWith(ZIP_EXTENSION))
        .map(MapZipReaderUtil::findGameXmlFilesInZip)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static List<URI> findAllUnZippedXmlFiles() {
    return FileUtils.listFiles(ClientFileSystemHelper.getUserMapsFolder()).stream()
        .filter(File::isDirectory)
        .map(AvailableGamesFileSystemReader::getDirectoryUris)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }

  private static List<URI> getDirectoryUris(final File mapDir) {
    // use contents under a "mapDir/map" folder if present, otherwise use the "mapDir/" contents
    // directly
    final File mapFolder = new File(mapDir, "map");

    final File parentFolder = mapFolder.exists() ? mapFolder : mapDir;
    final File games = new File(parentFolder, "games");
    return FileUtils.listFiles(games).stream()
        .parallel()
        .filter(File::isFile)
        .filter(game -> game.getName().toLowerCase().endsWith("xml"))
        .map(File::toURI)
        .collect(Collectors.toList());
  }

  public static void addNewMapToCache(final File installLocation) {
    if (availableGamesListCache == null) {
      parseMapFiles();
    }

    if (installLocation.getName().endsWith(ZIP_EXTENSION)) {
      mapXmlsGameNamesByUri(MapZipReaderUtil.findGameXmlFilesInZip(installLocation))
          .forEach(availableGamesListCache::add);
    }
  }
}
