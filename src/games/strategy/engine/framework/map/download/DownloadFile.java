package games.strategy.engine.framework.map.download;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.io.Files;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;

/**
 * Keeps track of the state for a file download from a URL.
 * This class notifies listeners as appropriate while download state changes.
 * A 'DownloadStrategy' does the heavy lifting URL download work.
 *
 * @see MapDownloadStrategy
 */
public class DownloadFile {

  public enum DownloadState {
    NOT_STARTED, DOWNLOADING, CANCELLED, DONE
  }

  private final List<Runnable> downloadCompletedListeners;
  private final Consumer<Integer> progressUpdateListener;
  private final DownloadFileDescription downloadDescription;
  private volatile DownloadState state = DownloadState.NOT_STARTED;

  /**
   * Creates a new FileDownload object.
   * Does not actually start the download, call 'startAsyncDownload()' to start the download.
   *
   * @param download The details of what to download
   * @param progressUpdateListener Called periodically while download progress is made.
   * @param completionListener Called when the File download is complete.
   */
  public DownloadFile(final DownloadFileDescription download, final Consumer<Integer> progressUpdateListener,
      final Runnable completionListener) {
    this(download, progressUpdateListener);
    this.addDownloadCompletedListener(completionListener);
  }

  protected DownloadFile(final DownloadFileDescription download, final Consumer<Integer> progressUpdateListener) {
    this.downloadDescription = download;
    this.progressUpdateListener = progressUpdateListener;
    this.downloadCompletedListeners = new ArrayList<>();
  }

  public void startAsyncDownload() {
    final File fileToDownloadTo = ClientFileSystemHelper.createTempFile();
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
        final URL url = downloadDescription.newURL();
        try {
          DownloadUtils.downloadFile(url, fileToDownloadTo);
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

  protected DownloadState getDownloadState() {
    return state;
  }

  public void cancelDownload() {
    if (!isDone()) {
      state = DownloadState.CANCELLED;
    }
  }

  public boolean isDone() {
    return state == DownloadState.CANCELLED || state == DownloadState.DONE;
  }

  public boolean isInProgress() {
    return state == DownloadState.DOWNLOADING;
  }

  public boolean isWaiting() {
    return state == DownloadState.NOT_STARTED;
  }

  public void addDownloadCompletedListener(final Runnable listener) {
    downloadCompletedListeners.add(listener);
  }
}
