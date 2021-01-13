package games.strategy.engine.framework.map.file.system.loader;

import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.ui.DefaultGameChooserEntry;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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
class AvailableGamesFileSystemReader {

  static synchronized DownloadedMaps parseMapFiles() {
    final List<URI> xmlFiles = findAllGameXmlFiles();
    final Collection<DefaultGameChooserEntry> gameChooserEntries = mapXmlsGameNamesByUri(xmlFiles);
    return new DownloadedMaps(gameChooserEntries);
  }

  private Collection<DefaultGameChooserEntry> mapXmlsGameNamesByUri(
      final Collection<URI> fileList) {
    return fileList.stream()
        .map(DefaultGameChooserEntry::newDefaultGameChooserEntry)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .peek(entry -> log.debug("Found game: " + entry.getGameName() + " @ " + entry.getUri()))
        .collect(Collectors.toList());
  }

  private static List<URI> findAllGameXmlFiles() {
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
}
