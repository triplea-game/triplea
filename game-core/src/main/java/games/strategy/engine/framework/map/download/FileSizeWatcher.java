package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.function.Consumer;
import org.triplea.java.Interruptibles;

/**
 * A class that will monitor the size of a file. Inputs are a file and a consumer, the file is
 * polled in a new thread for its file size which is then passed to the consumer.
 */
final class FileSizeWatcher {
  private final File fileToWatch;
  private final Consumer<Long> progressListener;
  private volatile boolean stop = false;

  FileSizeWatcher(final File fileToWatch, final Consumer<Long> progressListener) {
    this.fileToWatch = fileToWatch;
    this.progressListener = progressListener;
    new Thread(newRunner()).start();
  }

  void stop() {
    stop = true;
  }

  private Runnable newRunner() {
    return () -> {
      while (!stop) {
        progressListener.accept(fileToWatch.length());
        if (!Interruptibles.sleep(50)) {
          break;
        }
      }
    };
  }
}
