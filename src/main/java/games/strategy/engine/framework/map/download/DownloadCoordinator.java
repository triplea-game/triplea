package games.strategy.engine.framework.map.download;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.util.ThreadUtil;

/**
 * Class that accepts and queues download requests. Download requests are started in background
 * thread, this class ensures N are in progress until all are done.
 */
public class DownloadCoordinator {

  private static final int MAX_CONCURRENT_DOWNLOAD = 3;

  private final Map<DownloadFile, Runnable> downloadMap = new HashMap<>();
  private final Set<DownloadFileDescription> downloadSet = new HashSet<>();

  private volatile boolean cancelled = false;

  DownloadCoordinator() {
    new Thread(() -> {
      while (!cancelled) {
        try {
          startNextDownloads();
          // pause for a brief while before the next iteration, helps avoid a Github too many requests error
          ThreadUtil.sleep(250);
        } catch (final Exception e) {
          ClientLogger.logQuietly(e);
          throw e;
        }
      }
    }).start();
  }

  private void startNextDownloads() {
    final long downloadingCount = countDownloadsInProgress();
    if (downloadMap != null && downloadingCount < MAX_CONCURRENT_DOWNLOAD) {
      startNextDownload();
    }
  }


  private long countDownloadsInProgress() {
    return count(download -> download.isInProgress());
  }


  private long count(final Predicate<DownloadFile> filter) {
    return downloadMap.keySet().stream().filter(filter).count();
  }

  private void startNextDownload() {
    for (final Map.Entry<DownloadFile, Runnable> download : downloadMap.entrySet()) {
      if (download.getKey().isWaiting()) {
        new Thread(download.getValue()).start();
        download.getKey().startAsyncDownload(ClientFileSystemHelper.createTempFile());
        break;
      }
    }
  }

  /**
   * Queues up a download request, sending notification when the download is started, when the download progress is
   * updated, and when the download is complete.
   *
   * @param download A single map download to queue, may or may not be started immediately.
   * @param startedListener A listener that is called when this specific download is started.
   * @param progressUpdateListener A listener for progress updates, the value passed to the progress listener will be
   *        the size of the downloaded file in bytes.
   * @param completionListener A listener that is called when this specific download finishes.
   */
  void accept(
      final DownloadFileDescription download,
      final Runnable startedListener,
      final Consumer<Long> progressUpdateListener,
      final Runnable completionListener) {
    // To avoid double acceptance, hold a lock while we check the 'downloadSet'
    synchronized (this) {
      if (downloadSet.contains(download)) {
        return;
      } else {
        downloadSet.add(download);
      }
    }

    final DownloadFile downloadFile = new DownloadFile(download, progressUpdateListener, completionListener);
    downloadMap.put(downloadFile, startedListener);
  }

  /**
   * Will prevent any new downloads from starting. Downloads in progress will continue, but they
   * are downloaded to a temp file which will not be moved after cancel.
   */
  public void cancelDownloads() {
    cancelled = true;
    for (final DownloadFile download : downloadMap.keySet()) {
      download.cancelDownload();
    }
  }
}
