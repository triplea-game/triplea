package games.strategy.engine.framework.map.download;

import java.util.Optional;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import org.apache.commons.io.FileUtils;

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
    progressBar.setStringPainted(true);
  }

  private void requestDownloadLength() {
    new Thread(() -> downloadLength = DownloadConfiguration.downloadLengthReader().getDownloadLength(download.getUrl()))
        .start();
  }

  void downloadStarted() {
    updateProgressBarWithCurrentLength("Pending...", 0L);
  }

  private void updateProgressBarWithCurrentLength(final String toolTipText, final long currentLength) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(true);
      progressBar.setString(FileUtils.byteCountToDisplaySize(currentLength));
      progressBar.setToolTipText(toolTipText);
    });
  }

  private void updateProgressBarWithPercentComplete(final String toolTipText, final int percentComplete) {
    SwingUtilities.invokeLater(() -> {
      progressBar.setIndeterminate(false);
      progressBar.setString(null);
      progressBar.setValue(percentComplete);
      progressBar.setToolTipText(toolTipText);
    });
  }

  void downloadUpdated(final long currentLength) {
    final String toolTipText = String.format("Installing to: %s", download.getInstallLocation());
    OptionalUtils.ifPresentOrElse(downloadLength,
        totalLength -> updateProgressBarWithPercentComplete(toolTipText, percentComplete(currentLength, totalLength)),
        () -> updateProgressBarWithCurrentLength(toolTipText, currentLength));
  }

  private static int percentComplete(final long currentLength, final long totalLength) {
    return (int) (currentLength * MAX_PROGRESS_VALUE / totalLength);
  }

  void downloadCompleted() {
    updateProgressBarWithPercentComplete(
        String.format("Installed to: %s", download.getInstallLocation()),
        MAX_PROGRESS_VALUE);
  }
}
