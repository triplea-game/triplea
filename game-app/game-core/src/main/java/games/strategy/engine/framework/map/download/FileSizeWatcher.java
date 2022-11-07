package games.strategy.engine.framework.map.download;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.triplea.java.Interruptibles;
import org.triplea.java.ThreadRunner;

/**
 * A class that will monitor the size of a file. Inputs are a file and a consumer, the file is
 * polled in a new thread for its file size which is then passed to the consumer.
 */
@Slf4j
final class FileSizeWatcher {
  private final Path fileToWatch;
  private final Consumer<Long> progressListener;
  private volatile boolean stop = false;

  FileSizeWatcher(final Path fileToWatch, final Consumer<Long> progressListener) {
    this.fileToWatch = fileToWatch;
    this.progressListener = progressListener;
    ThreadRunner.runInNewThread(newRunner());
  }

  void stop() {
    stop = true;
  }

  private Runnable newRunner() {
    return () -> {
      while (!stop) {
        try {
          if (Files.exists(fileToWatch)) {
            progressListener.accept(Files.size(fileToWatch));
          }
        } catch (final IOException e) {
          log.error("Failed to read filesize", e);
        }
        if (!Interruptibles.sleep(50)) {
          break;
        }
      }
    };
  }
}
