package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.function.Consumer;

import games.strategy.util.ThreadUtil;

/**
 * A class that will monitor the size of a file. Inputs are a file and a consumer,
 * the file is polled in a new thread for its file size which is then passed to the
 * consumer.
 */
public class FileSizeWatcher {

  private final File fileToWatch;
  private final Consumer<Integer> progressListener;
  private boolean stop = false;

  public FileSizeWatcher(final File fileToWatch, final Consumer<Integer> progressListener) {
    this.fileToWatch = fileToWatch;
    this.progressListener = progressListener;
    (new Thread(createRunner())).start();
  }

  public void stop() {
    stop = true;
  }

  private Runnable createRunner() {
    return () -> {
      while (!stop) {
        progressListener.accept((int) fileToWatch.length());
        ThreadUtil.sleep(50);
      }
    };
  }
}
