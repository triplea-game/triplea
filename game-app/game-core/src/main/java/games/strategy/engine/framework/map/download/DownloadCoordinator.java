package games.strategy.engine.framework.map.download;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

/// Class that accepts and queues download requests. Download requests are started in background
/// thread, this class ensures N are in progress until all are done.
@Slf4j
public final class DownloadCoordinator {
  public static final DownloadCoordinator instance =
      new DownloadCoordinator(DownloadConfiguration.contentReader());

  private static final int MAX_CONCURRENT_DOWNLOADS = 3;

  private final Object lock = new Object();
  private final ContentReader contentReader;

  private final Queue<DownloadFile> pendingDownloads = new ArrayDeque<>();
  private final Set<DownloadFile> activeDownloads = new HashSet<>();
  private final Set<DownloadFile> terminatedDownloads = new HashSet<>();

  private final List<DownloadListener> downloadListeners = new CopyOnWriteArrayList<>();

  DownloadCoordinator(final ContentReader contentReader) {
    this.contentReader = contentReader;
  }

  void addDownloadListener(final DownloadListener downloadListener) {
    downloadListeners.add(downloadListener);
  }

  void removeDownloadListener(final DownloadListener downloadListener) {
    downloadListeners.remove(downloadListener);
  }

  void accept(final MapDownloadItem download) {
    synchronized (lock) {
      if (isNewDownload(download)) {
        pendingDownloads.add(new DownloadFile(download, new Listener(), contentReader));
        updateQueue();
      }
    }
  }

  private boolean isNewDownload(final MapDownloadItem download) {
    assert Thread.holdsLock(lock);

    return !containsDownload(pendingDownloads, download)
        && !containsDownload(activeDownloads, download);
  }

  private static boolean containsDownload(
      final Collection<DownloadFile> downloadFiles, final MapDownloadItem download) {
    return downloadFiles.stream()
        .map(DownloadFile::getDownload)
        .anyMatch(queued -> isSameMap(queued, download));
  }

  /**
   * By the map's name: a maps window opened again reads the listing again, which has its own items
   * of the maps, whose details may have changed since; one map is downloaded once, into one folder.
   */
  private static boolean isSameMap(final MapDownloadItem one, final MapDownloadItem other) {
    return one.getMapName().equals(other.getMapName());
  }

  /// Starts any downloads that can be started.
  private void updateQueue() {
    assert Thread.holdsLock(lock);

    if (activeDownloads.size() < MAX_CONCURRENT_DOWNLOADS && !pendingDownloads.isEmpty()) {
      final DownloadFile downloadFile = pendingDownloads.remove();
      downloadFile.startAsyncDownload();
      activeDownloads.add(downloadFile);
    }
  }

  /// Cancels the download of the map, whether it waits or runs; the next waiting one may start. A
  /// download done and unpacking its map is not cancelled: its map would be half there.
  void cancel(final MapDownloadItem download) {
    synchronized (lock) {
      cancelIn(pendingDownloads, download);
      cancelIn(activeDownloads, download);
      updateQueue();
    }
  }

  /** Cancels the map's downloads that wait or run. */
  private void cancelIn(
      final Collection<DownloadFile> downloadFiles, final MapDownloadItem download) {
    assert Thread.holdsLock(lock);

    final Iterator<DownloadFile> iterator = downloadFiles.iterator();
    while (iterator.hasNext()) {
      final DownloadFile downloadFile = iterator.next();
      if (isSameMap(downloadFile.getDownload(), download) && !downloadFile.isDone()) {
        iterator.remove();
        downloadFile.cancelDownload();
        terminatedDownloads.add(downloadFile);
      }
    }
  }

  Set<DownloadFile> pollTerminatedDownloads() {
    synchronized (lock) {
      final Set<DownloadFile> set = new HashSet<>(terminatedDownloads);
      terminatedDownloads.clear();
      return set;
    }
  }

  Set<DownloadFile> getPendingDownloads() {
    synchronized (lock) {
      return new HashSet<>(pendingDownloads);
    }
  }

  Set<DownloadFile> getDownloads() {
    synchronized (lock) {
      final Set<DownloadFile> result = getPendingDownloads();
      result.addAll(activeDownloads);
      result.addAll(pollTerminatedDownloads());
      return result;
    }
  }

  /**
   * Tells every listener, also when one fails: else the listeners after it never heard of the end,
   * and the next download never started.
   */
  private void tellEach(final Consumer<DownloadListener> news) {
    for (final DownloadListener listener : downloadListeners) {
      try {
        news.accept(listener);
      } catch (final RuntimeException e) {
        // not as an error, which shows a dialog: progress is told every few milliseconds
        log.info("A listener of the map downloads failed", e);
      }
    }
  }

  private final class Listener implements DownloadListener {
    @Override
    public void downloadUpdated(final MapDownloadItem download, final long bytesReceived) {
      tellEach(it -> it.downloadUpdated(download, bytesReceived));
    }

    @Override
    public void downloadComplete(final MapDownloadItem download) {
      tellEach(it -> it.downloadComplete(download));
      startNextDownload(download);
    }

    @Override
    public void downloadFailed(final MapDownloadItem download) {
      tellEach(it -> it.downloadFailed(download));
      startNextDownload(download);
    }

    /**
     * Moves the finished download out of the running ones, which makes room for the next one: this
     * one, not any done, as another may still unpack its map.
     */
    private void startNextDownload(final MapDownloadItem finished) {
      synchronized (lock) {
        final Iterator<DownloadFile> iterator = activeDownloads.iterator();
        while (iterator.hasNext()) {
          final DownloadFile downloadFile = iterator.next();
          if (downloadFile.isDone() && isSameMap(downloadFile.getDownload(), finished)) {
            iterator.remove();
            terminatedDownloads.add(downloadFile);
            break;
          }
        }
        updateQueue();
      }
    }
  }
}
