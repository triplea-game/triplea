package games.strategy.engine.framework.map.download;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.ZippedMapsExtractor;
import java.io.IOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.io.FileUtils;
import org.triplea.java.ThreadRunner;
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
    DONE
  }

  private final MapDownloadItem download;
  private final DownloadListener downloadListener;
  private volatile DownloadState state = DownloadState.NOT_STARTED;

  DownloadFile(final MapDownloadItem download, final DownloadListener downloadListener) {
    this.download = download;
    this.downloadListener = downloadListener;
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
            DownloadConfiguration.contentReader()
                .downloadToFile(download.getDownloadUrl(), targetTempFileToDownloadTo);
          } catch (final IOException e) {
            log.error("Failed to download: " + download.getDownloadUrl(), e);
            return;
          } finally {
            watcher.stop();
          }

          if (state == DownloadState.CANCELLED) {
            return;
          }

          state = DownloadState.DONE;

          // extract map, if successful and does not have a 'map.yml' file, generate one.
          ZippedMapsExtractor.unzipMap(targetTempFileToDownloadTo)
              .ifPresent(
                  installedMap -> {
                    // create a map description YAML file for the map if it does not contain one
                    if (MapDescriptionYaml.fromMap(installedMap).isEmpty()) {
                      MapDescriptionYaml.generateForMap(installedMap);
                    }
                  });

          downloadListener.downloadComplete(download);
        });
  }

  /**
   * Strips invalid and dangerous file system characters from a map name. The map name is used to
   * create a folder, we would not want a map named something like "/bin/bash" or "c:\\"
   */
  @VisibleForTesting
  static String normalizeMapName(final String mapName) {
    return mapName
        .replaceAll(" ", "_")
        .replaceAll("[&;:.,/]", "")
        .replaceAll("\\\\", "")
        .replaceAll("\\|", "")
        .replaceAll("\\]", "")
        .replaceAll("\\[", "")
        .replaceAll("\\*", "")
        .replaceAll("\"", "");
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
    return state == DownloadState.CANCELLED || state == DownloadState.DONE;
  }
}
