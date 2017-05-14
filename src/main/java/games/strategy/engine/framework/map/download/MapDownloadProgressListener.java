package games.strategy.engine.framework.map.download;

import java.net.URL;
import java.util.Optional;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 * A listener of map download progress events that updates the associated controls in the UI.
 *
 * <p>
 * Instances of this class are thread safe.
 * </p>
 */
final class MapDownloadProgressListener {
  private static final int MIN_PROGRESS_VALUE = 0;

  private static final int MAX_PROGRESS_VALUE = 100;

  private final JProgressBar progressBar;

  private volatile Optional<Long> downloadLength = Optional.empty();

  MapDownloadProgressListener(final JProgressBar progressBar) {
    this.progressBar = progressBar;
  }

  void downloadStarted(final URL url) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setMinimum(MIN_PROGRESS_VALUE);
      progressBar.setMaximum(MAX_PROGRESS_VALUE);
      progressBar.setIndeterminate(true);
      progressBar.setStringPainted(false);
    });

    downloadLength = DownloadUtils.getDownloadLength(url);
  }

  void downloadUpdated(final long currentLength) {
    downloadLength.ifPresent(totalLength -> SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(false);
      progressBar.setValue((int) (currentLength * MAX_PROGRESS_VALUE / totalLength));
      progressBar.setStringPainted(true);
    }));
  }

  void downloadCompleted() {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(false);
      progressBar.setValue(MAX_PROGRESS_VALUE);
      progressBar.setStringPainted(true);
    });
  }
}
