package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.io.FileUtils;

/**
 * Reads from file system to find all available games. We then shallow parse each available game to
 * find the game name and we create a mapping of game name to location on disk. Values read from
 * disk are cached (can be updated by calling "{@code addNewMapToCache}).
 */
@UtilityClass
@Slf4j
public class AvailableGamesFileSystemReader {

  private static final String ZIP_EXTENSION = ".zip";

  private static AvailableGamesList availableGamesListCache;

  public static synchronized AvailableGamesList parseMapFiles() {
    if (availableGamesListCache == null) {
      final Set<DefaultGameChooserEntry> entries = new HashSet<>();
      entries.addAll(mapXmlsGameNamesByUri(findAllZippedXmlFiles()));
      entries.addAll(mapXmlsGameNamesByUri(findAllUnZippedXmlFiles()));
      availableGamesListCache = new AvailableGamesList(entries);
      entries.forEach(
          entry -> log.debug("Found game: " + entry.getGameName() + " @ " + entry.getUri()));
    }

    return availableGamesListCache;
  }

  public static void refreshMapFileCache() {
    availableGamesListCache = null;
    new Thread(AvailableGamesFileSystemReader::parseMapFiles).start();
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

  private static Collection<URI> getDirectoryUris(final File mapDir) {
    // finds all XML files under mapDir
    try {
      return Files.find(
              mapDir.toPath(),
              8,
              (path, basicAttributes) ->
                  basicAttributes.isRegularFile() && path.getFileName().toString().endsWith(".xml"))
          .map(Path::toUri)
          .collect(Collectors.toList());
    } catch (final IOException e) {
      log.warn("Unable to read map folder: " + mapDir.getAbsolutePath() + "," + e.getMessage(), e);
      return List.of();
    }
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
