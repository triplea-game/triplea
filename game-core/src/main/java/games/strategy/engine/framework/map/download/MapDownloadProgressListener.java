package games.strategy.engine.framework.map.download;

import java.util.Optional;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import games.strategy.util.OptionalUtils;

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

  private final DownloadFileDescription download;
  private volatile Optional<Long> downloadLength = Optional.empty();
  private final JProgressBar progressBar;

  MapDownloadProgressListener(final DownloadFileDescription download, final JProgressBar progressBar) {
    this.download = download;
    this.progressBar = progressBar;

    initializeProgressBar();
    requestDownloadLength();
  }

  private void initializeProgressBar() {
    assert SwingUtilities.isEventDispatchThread();

    progressBar.setMinimum(MIN_PROGRESS_VALUE);
    progressBar.setMaximum(MAX_PROGRESS_VALUE);
  }

  private void requestDownloadLength() {
    new Thread(() -> downloadLength = DownloadUtils.getDownloadLength(download.getUrl())).start();
  }

  void downloadStarted() {
    updateProgressBar("Pending...");
  }

  private void updateProgressBar(final String toolTipText) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(true);
      progressBar.setStringPainted(false);
      progressBar.setToolTipText(toolTipText);
    });
  }

  private void updateProgressBar(final String toolTipText, final int value) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(false);
      progressBar.setValue(value);
      progressBar.setStringPainted(true);
      progressBar.setToolTipText(toolTipText);
    });
  }

  void downloadUpdated(final long currentLength) {
    final String toolTipText = String.format("Installing to: %s", download.getInstallLocation());
    OptionalUtils.ifPresentOrElse(downloadLength,
        totalLength -> updateProgressBar(toolTipText, percentComplete(currentLength, totalLength)),
        () -> updateProgressBar(toolTipText));
  }

  private static int percentComplete(final long currentLength, final long totalLength) {
    return (int) ((currentLength * MAX_PROGRESS_VALUE) / totalLength);
  }

  void downloadCompleted() {
    updateProgressBar(String.format("Installed to: %s", download.getInstallLocation()), MAX_PROGRESS_VALUE);
  }
}
