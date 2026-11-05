package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.ZippedMapsExtractor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.io.ZipExtractor;
import org.triplea.java.ThreadRunner;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.map.description.file.MapDescriptionYaml;

/**
 * Keeps track of the state for a file download from a URL. This class notifies listeners as
 * appropriate while download state changes.
 */
@Slf4j
final class DownloadFile {
  @VisibleForTesting
  enum DownloadState {
    NOT_STARTED,
    DOWNLOADING,
    CANCELLED,
    DONE,
    FAILED
  }

  private final MapDownloadItem download;
  private final DownloadListener downloadListener;
  private final ContentReader contentReader;
  private final Function<Path, Optional<Path>> unzipMap;
  private volatile DownloadState state = DownloadState.NOT_STARTED;

  DownloadFile(final MapDownloadItem download, final DownloadListener downloadListener) {
    this(
        download,
        downloadListener,
        DownloadConfiguration.contentReader(),
        ZippedMapsExtractor::unzipMap);
  }

  @VisibleForTesting
  DownloadFile(
      final MapDownloadItem download,
      final DownloadListener downloadListener,
      final ContentReader contentReader,
      final Function<Path, Optional<Path>> unzipMap) {
    this.download = download;
    this.downloadListener = downloadListener;
    this.contentReader = contentReader;
    this.unzipMap = unzipMap;
  }

  MapDownloadItem getDownload() {
    return download;
  }

  /**
   * Creates a thread that will download to a target temporary file, and once complete and if the
   * download state is not cancelled, it will then move the completed download temp file to:
   * 'downloadDescription.getInstallLocation()'.
   */
  void startAsyncDownload() {
    state = DownloadState.DOWNLOADING;
    ThreadRunner.runInNewThread(
        () -> {
          if (state == DownloadState.CANCELLED) {
            return;
          }

          @NonNls final String fileNameToWrite = normalizeMapName(download.getMapName()) + ".zip";

          final Path targetTempFileToDownloadTo =
              FileUtils.newTempFolder().resolve(fileNameToWrite);

          final FileSizeWatcher watcher =
              new FileSizeWatcher(
                  targetTempFileToDownloadTo,
                  bytesReceived -> downloadListener.downloadUpdated(download, bytesReceived));

          try {
            contentReader.downloadToFile(download.getDownloadUrl(), targetTempFileToDownloadTo);
          } catch (final IOException e) {
            log.error("Failed to download: {}", download.getDownloadUrl(), e);
            notifyDownloadFailed();
            return;
          } finally {
            watcher.stop();
          }

          if (state == DownloadState.CANCELLED) {
            return;
          }

          // extract map, if successful and does not have a 'map.yml' file, generate one.
          final Optional<Path> installedMap;
          try {
            installedMap = unzipMap.apply(targetTempFileToDownloadTo);
          } catch (final ZipExtractor.ZipReadException
              | ZipExtractor.FileSystemException
              | ZipExtractor.ZipSecurityException e) {
            log.error("Failed to extract map: {}", targetTempFileToDownloadTo, e);
            notifyDownloadFailed();
            return;
          }
          if (installedMap.isEmpty() || moveUpSubfolderFilesHasErrors(installedMap.get())) {
            notifyDownloadFailed();
            return;
          }

          if (MapDescriptionYaml.fromMap(installedMap.get()).isEmpty()) {
            MapDescriptionYaml.generateForMap(installedMap.get());
          }

          state = DownloadState.DONE;
          downloadListener.downloadComplete(download);
        });
  }

  /// Reports the failure unless the download was cancelled in the meantime; a cancelled download is
  /// not an error and must not free the slot of another download.
  private void notifyDownloadFailed() {
    if (state == DownloadState.CANCELLED) {
      return;
    }
    state = DownloadState.FAILED;
    downloadListener.downloadFailed(download);
  }

  /**
   * If {@code installedMap} has only one unique subfolder, this subfolder level is remove, i.e.,
   * all files are moved up and the subfolder is deleted.
   *
   * @param installedMap target of unzip action
   * @return whether there has been an error on moving the subfolder files or deleting the empty
   *     subfolder afterward
   */
  private static boolean moveUpSubfolderFilesHasErrors(Path installedMap) {
    final Collection<Path> extractedFiles = FileUtils.listFiles(installedMap);
    if (extractedFiles.size() == 1 && Files.isDirectory(CollectionUtils.getAny(extractedFiles))) {
      // unzipped file contains a single folder that contains all the map files
      final Path uniqueSubFolder = CollectionUtils.getAny(extractedFiles);
      for (Path containedFile : FileUtils.listFiles(uniqueSubFolder)) {
        final Path levelUpFilePath =
            Paths.get(installedMap.toString() + File.separator + containedFile.getFileName());
        try {
          Files.move(containedFile, levelUpFilePath);
        } catch (IOException e) {
          log.error("Failed to move file: {} to {}", containedFile, levelUpFilePath, e);
          return true;
        }
      }
      try {
        Files.delete(uniqueSubFolder);
      } catch (IOException e) {
        log.error("Failed to delete subfolder: {}", uniqueSubFolder, e);
        return true;
      }
    }
    return false;
  }

  /**
   * Strips invalid and dangerous file system characters from a map name. The map name is used to
   * create a folder, we would not want a map named something like "/bin/bash" or "c:\\"
   */
  @VisibleForTesting
  static String normalizeMapName(final String mapName) {
    return mapName
        .replace(" ", "_")
        .replaceAll("[&;:.,/]", "")
        .replace("\\", "")
        .replace("|", "")
        .replace("]", "")
        .replace("[", "")
        .replace("*", "")
        .replace("\"", "");
  }

  @VisibleForTesting
  DownloadState getDownloadState() {
    return state;
  }

  void cancelDownload() {
    if (!isDone()) {
      state = DownloadState.CANCELLED;
    }
  }

  boolean isDone() {
    return state == DownloadState.CANCELLED
        || state == DownloadState.DONE
        || state == DownloadState.FAILED;
  }
}
