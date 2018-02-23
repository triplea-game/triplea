package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/**
 * Keeps track of the state for a file download from a URL.
 * This class notifies listeners as appropriate while download state changes.
 */
final class DownloadFile {
  @VisibleForTesting
  enum DownloadState {
    NOT_STARTED, DOWNLOADING, CANCELLED, DONE
  }

  private final DownloadFileDescription download;
  private final DownloadListener downloadListener;
  private volatile DownloadState state = DownloadState.NOT_STARTED;

  DownloadFile(final DownloadFileDescription download, final DownloadListener downloadListener) {
    this.download = download;
    this.downloadListener = downloadListener;

    SwingUtilities.invokeLater(() -> downloadListener.downloadStarted(download));
  }

  DownloadFileDescription getDownload() {
    return download;
  }

  void startAsyncDownload() {
    state = DownloadState.DOWNLOADING;
    createDownloadThread().start();
  }

  /*
   * Creates a thread that will download to a target temporary file, and once
   * complete and if the download state is not cancelled, it will then move
   * the completed download temp file to: 'downloadDescription.getInstallLocation()'
   */
  private Thread createDownloadThread() {
    return new Thread(() -> {
      if (state == DownloadState.CANCELLED) {
        return;
      }

      final File tempFile = newTempFile();
      final FileSizeWatcher watcher = new FileSizeWatcher(
          tempFile,
          bytesReceived -> downloadListener.downloadUpdated(download, bytesReceived));
      try {
        DownloadUtils.downloadToFile(download.getUrl(), tempFile);
      } catch (final IOException e) {
        ClientLogger.logError("Failed to download: " + download.getUrl(), e);
        return;
      } finally {
        watcher.stop();
      }

      if (state == DownloadState.CANCELLED) {
        return;
      }

      state = DownloadState.DONE;

      try {
        Files.move(tempFile, download.getInstallLocation());
      } catch (final IOException e) {
        ClientLogger.logError(
            String.format("Failed to move downloaded file (%s) to: %s", tempFile, download.getInstallLocation()),
            e);
        return;
      }

      final DownloadFileProperties props = new DownloadFileProperties();
      props.setFrom(download);
      DownloadFileProperties.saveForZip(download.getInstallLocation(), props);

      downloadListener.downloadStopped(download);
    });
  }

  private static File newTempFile() {
    final File file = ClientFileSystemHelper.createTempFile();
    file.deleteOnExit();
    return file;
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
    return (state == DownloadState.CANCELLED) || (state == DownloadState.DONE);
  }
}
