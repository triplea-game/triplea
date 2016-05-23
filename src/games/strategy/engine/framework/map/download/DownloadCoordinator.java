package games.strategy.engine.framework.map.download;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import games.strategy.common.swing.SwingComponents;
import games.strategy.util.ThreadUtil;

/**
 * Class that accepts and queues download requests. Download requests are started in background
 * thread, this class ensures N are in progress until all are done.
 */
public class DownloadCoordinator {

  private static final int MAX_CONCURRENT_DOWNLOAD = 3;

  private final List<DownloadFile> downloadList = Lists.newCopyOnWriteArrayList();
  private final Set<DownloadFileDescription> downloadSet = Sets.newHashSet();

  private boolean downloadPromptAlreadyShown = false;
  private volatile boolean cancelled = false;

  public DownloadCoordinator() {
    final Thread downloadFileStarter = createDownloadFileStarterThread();
    downloadFileStarter.start();
  }

  private final Thread createDownloadFileStarterThread() {
    return new Thread(() -> {
      while (!cancelled) {
        try {
          startNextDownloads();
          // pause for a brief while before the next iteration, helps avoid a Github too many requests error
          ThreadUtil.sleep(250);
        } catch (Exception e) {
          e.printStackTrace();
          throw e;
        }
      }
    });
  }

  private void startNextDownloads() {
    long downloadingCount = countDownloadsInProgress();
    if (downloadList != null && downloadingCount < MAX_CONCURRENT_DOWNLOAD) {
      startNextDownload();
    }
  }


  private long countDownloadsInProgress() {
    return count(download -> download.isInProgress());
  }


  private long count(Predicate<DownloadFile> filter) {
    return downloadList.stream().filter(filter).count();
  }

  private void startNextDownload() {
    for (DownloadFile download : downloadList) {
      if (download.isWaiting()) {
        download.startAsyncDownload();
        break;
      }
    }
  }

  /**
   * Queues up a download request, sending notification to a progress listener and a final notification
   * to a download complete listener.
   *
   * @param download A single map download to queue, may or may not be started immediately.
   * @param progressUpdateListener A listener for progress updates, the value passed to the progress listener will be
   *        the size of the downloaded file in bytes.
   * @param completionListener A listener that is called when this specific download finishes.
   */
  public void accept(DownloadFileDescription download, Consumer<Integer> progressUpdateListener,
      Runnable completionListener) {
    // To avoid double acceptance, hold a lock while we check the 'downloadSet'
    synchronized (this) {
      if (download.isDummyUrl() || downloadSet.contains(download)) {
        return;
      } else {
        downloadSet.add(download);
        downloadPromptAlreadyShown = false;
      }
    }
    downloadList.add(new DownloadFile(download, progressUpdateListener, completionListener));
  }

  /**
   * Will prevent any new downloads from starting. Downloads in progress will continue, but they
   * are downloaded to a temp file which will not be moved after cancel.
   */
  public void cancelDownloads() {
    cancelled = true;
    for (DownloadFile download : downloadList) {
      download.cancelDownload();
    }
  }
}
