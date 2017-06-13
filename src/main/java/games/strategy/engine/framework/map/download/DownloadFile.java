package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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

  private final Runnable downloadStartedListener;
  private final List<Runnable> downloadCompletedListeners = new ArrayList<>();
  private Consumer<Long> progressUpdateListener;
  private final DownloadFileDescription downloadDescription;
  private volatile DownloadState state = DownloadState.NOT_STARTED;

  /**
   * Creates a new DownloadFile object.
   * Does not actually start the download, call 'startAsyncDownload()' to start the download.
   *
   * @param download The details of what to download
   * @param progressUpdateListener Called periodically while download progress is made.
   * @param completionListener Called when the File download is complete.
   */
  DownloadFile(final DownloadFileDescription download, Runnable startedListener, final Consumer<Long> progressUpdateListener,
      final Runnable completionListener) {
    this.downloadStartedListener = startedListener;
    this.downloadDescription = download;
    this.progressUpdateListener = progressUpdateListener;
    addDownloadCompletedListener(completionListener);
  }

  /**
   * @param fileToDownloadTo The intermediate file to which the download is saved; must not be {@code null}. If the
   *        download is successful, this file will be moved to the install location. If the download is cancelled, this
   *        file <strong>WILL NOT</strong> be deleted.
   */
  void startAsyncDownload(final File fileToDownloadTo) {
    new Thread(downloadStartedListener).start();
    final FileSizeWatcher watcher = new FileSizeWatcher(fileToDownloadTo, progressUpdateListener);
    addDownloadCompletedListener(() -> watcher.stop());
    state = DownloadState.DOWNLOADING;
    createDownloadThread(fileToDownloadTo).start();
  }

  /*
   * Creates a thread that will download to a target temporary file, and once
   * complete and if the download state is not cancelled, it will then move
   * the completed download temp file to: 'downloadDescription.getInstallLocation()'
   */
  private Thread createDownloadThread(final File fileToDownloadTo) {
    return new Thread(() -> {
      if (state != DownloadState.CANCELLED) {
        final String url = downloadDescription.getUrl();
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
          Files.move(fileToDownloadTo, downloadDescription.getInstallLocation());

          final DownloadFileProperties props = new DownloadFileProperties();
          props.setFrom(downloadDescription);
          DownloadFileProperties.saveForZip(downloadDescription.getInstallLocation(), props);

        } catch (final Exception e) {
          final String msg = "Failed to move downloaded file (" + fileToDownloadTo.getAbsolutePath() + ") to: "
              + downloadDescription.getInstallLocation().getAbsolutePath();
          ClientLogger.logError(msg, e);
        }
      }
      // notify listeners we finished the download
      downloadCompletedListeners.forEach(e -> e.run());
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

  private void addDownloadCompletedListener(final Runnable listener) {
    downloadCompletedListeners.add(listener);
  }
}
