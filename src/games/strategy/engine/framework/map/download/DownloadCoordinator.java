package games.strategy.engine.framework.map.download;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import games.strategy.common.swing.SwingComponents;

/**
 * Class that accepts and queues download requests. Download requests are started in background
 * thread, this class ensures N are in progress until all are done.
 */
public class DownloadCoordinator {

  private static final int MAX_CONCURRENT_DOWNLOAD = 3;

  private final List<DownloadFile> downloadList = Lists.newCopyOnWriteArrayList();
  private final Set<DownloadFileDescription> downloadSet = Sets.newHashSet();

  private Optional<Runnable> downloadCompleteAction = Optional.empty();

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
          // pause for a brief while before the next iteration
          try {
            Thread.sleep(50);
          } catch (InterruptedException e) {
          }

          if (shouldShowDownloadsFinishedPrompt()) {

            // TODO: we ignore the threading problem here, we may enter this code block multiple times.
            // It's a difficult situation to create for the user, and the worst case is multiple map download
            // complete dialogs appearing - which is not a big deal.
            downloadPromptAlreadyShown = true;
            Runnable confirmAction = () -> {
              if (downloadCompleteAction.isPresent()) {
                downloadCompleteAction.get().run();
              }
            };
            SwingComponents.promptUser("Map Downloads Completed",
                "Map downloads are complete, would you like to continue downloading more maps?", confirmAction);
          }
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

  private boolean shouldShowDownloadsFinishedPrompt() {
    boolean doneDownloading = countDownloadsInProgress() == 0;
    boolean downloadedSomething = downloadList.size() > 0;
    boolean noMoreDownloads = countWaiting() == 0;

    return !cancelled && !downloadPromptAlreadyShown && doneDownloading && downloadedSomething && noMoreDownloads;
  }

  private long countDownloadsInProgress() {
    return count(download -> download.isInProgress());
  }

  private long countWaiting() {
    return count(download -> download.isWaiting());
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
   * Set a callback object to be called when all queued downloads have completed.
   */
  public void setAllDownloadCompleteAction(Runnable closeAction) {
    this.downloadCompleteAction = Optional.of(closeAction);
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
