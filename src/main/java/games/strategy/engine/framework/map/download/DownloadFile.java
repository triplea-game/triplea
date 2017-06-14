package games.strategy.engine.framework.map.download;

import java.io.File;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Files;

import games.strategy.debug.ClientLogger;

/**
 * Keeps track of the state for a file download from a URL.
 * This class notifies listeners as appropriate while download state changes.
 */
final class DownloadFile {
  enum DownloadState {
    NOT_STARTED, DOWNLOADING, CANCELLED, DONE
  }

  private final DownloadFileDescription download;
  private final DownloadListener downloadListener;
  private volatile DownloadState state = DownloadState.NOT_STARTED;

  DownloadFile(final DownloadFileDescription download, final DownloadListener downloadListener) {
    this.download = download;
    this.downloadListener = downloadListener;

    // TODO: consider running in a separate thread because this constructor is called while a lock is held
    // it works now because the listener calls SwingUtilities.invokeLater(), but that may not always be the case
    downloadListener.downloadStarted(download);
  }

  DownloadFileDescription getDownload() {
    return download;
  }

  /**
   * @param fileToDownloadTo The intermediate file to which the download is saved; must not be {@code null}. If the
   *        download is successful, this file will be moved to the install location. If the download is cancelled, this
   *        file <strong>WILL NOT</strong> be deleted.
   */
  void startAsyncDownload(final File fileToDownloadTo) {
    final FileSizeWatcher watcher = new FileSizeWatcher(
        fileToDownloadTo,
        bytesReceived -> downloadListener.downloadUpdated(download, bytesReceived));
    state = DownloadState.DOWNLOADING;
    createDownloadThread(watcher).start();
  }

  /*
   * Creates a thread that will download to a target temporary file, and once
   * complete and if the download state is not cancelled, it will then move
   * the completed download temp file to: 'downloadDescription.getInstallLocation()'
   */
  private Thread createDownloadThread(final FileSizeWatcher watcher) {
    return new Thread(() -> {
      final File fileToDownloadTo = watcher.getFile();
      if (state != DownloadState.CANCELLED) {
        final String url = download.getUrl();
        try {
          DownloadUtils.downloadToFile(url, fileToDownloadTo);
        } catch (final Exception e) {
          ClientLogger.logError("Failed to download: " + url, e);
        }
        if (state == DownloadState.CANCELLED) {
          return;
        }
        state = DownloadState.DONE;
        try {
          Files.move(fileToDownloadTo, download.getInstallLocation());

          final DownloadFileProperties props = new DownloadFileProperties();
          props.setFrom(download);
          DownloadFileProperties.saveForZip(download.getInstallLocation(), props);

        } catch (final Exception e) {
          final String msg = "Failed to move downloaded file (" + fileToDownloadTo.getAbsolutePath() + ") to: "
              + download.getInstallLocation().getAbsolutePath();
          ClientLogger.logError(msg, e);
        }
      }

      watcher.stop();
      // TODO: update code in this method so we pass reason (DONE, CANCELLED) to the listener
      downloadListener.downloadStopped(download);
    });
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

  boolean isInProgress() {
    return state == DownloadState.DOWNLOADING;
  }

  boolean isWaiting() {
    return state == DownloadState.NOT_STARTED;
  }
}
