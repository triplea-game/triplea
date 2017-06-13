package games.strategy.engine.framework.map.download;

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
  private final String uri;
  private final String tooltip;
  private volatile Optional<Long> downloadLength = Optional.empty();

  MapDownloadProgressListener(final String uri, final JProgressBar progressBar, String tooltip) {
    this.progressBar = progressBar;
    this.uri = uri;
    this.tooltip = tooltip;
  }

  void downloadStarted() {
    SwingUtilities.invokeLater(() -> {
      progressBar.setMinimum(MIN_PROGRESS_VALUE);
      progressBar.setMaximum(MAX_PROGRESS_VALUE);
      progressBar.setIndeterminate(true);
      progressBar.setStringPainted(false);
      progressBar.setToolTipText("Pending...");
    });
    downloadLength = DownloadUtils.getDownloadLength(uri);
  }

  void downloadUpdated(final long currentLength) {
    if (currentLength > 0) {
      downloadLength.ifPresent(totalLength -> SwingUtilities.invokeLater(() -> {
        progressBar.setIndeterminate(false);
        progressBar.setValue((int) (currentLength * MAX_PROGRESS_VALUE / totalLength));
        progressBar.setStringPainted(true);
      }));
      SwingUtilities.invokeLater(() -> progressBar.setToolTipText(tooltip));
    }
  }

  void downloadCompleted() {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(false);
      progressBar.setValue(MAX_PROGRESS_VALUE);
      progressBar.setStringPainted(true);
    });
  }
}
