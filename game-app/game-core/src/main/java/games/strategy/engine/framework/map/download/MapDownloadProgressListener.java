package games.strategy.engine.framework.map.download;

import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.triplea.http.client.maps.listing.MapDownloadItem;

/**
 * A listener of map download progress events that updates the associated controls in the UI.
 *
 * <p>Instances of this class are thread safe.
 */
final class MapDownloadProgressListener {
  private final MapDownloadItem download;
  private final JProgressBar progressBar;

  /** The amount total that we will be downloading. */
  private final long downloadLength;

  MapDownloadProgressListener(final MapDownloadItem download, final JProgressBar progressBar) {
    this.download = download;
    this.progressBar = progressBar;
    downloadLength = download.getDownloadSizeInBytes();
  }

  private void updateProgressBarWithPercentComplete(
      final String toolTipText, final int percentComplete) {
    SwingUtilities.invokeLater(
        () -> {
          progressBar.setIndeterminate(false);
          progressBar.setString(null);
          progressBar.setValue(Math.max(0, percentComplete));
          progressBar.setToolTipText(toolTipText);
        });
  }

  void downloadUpdated(final long currentLength) {
    final String toolTipText = "Installing..";

    updateProgressBarWithPercentComplete(
        toolTipText, percentComplete(currentLength, downloadLength));
  }

  private static int percentComplete(final long currentLength, final long totalLength) {
    return (int) (currentLength * 100 / totalLength);
  }

  void downloadCompleted() {
    updateProgressBarWithPercentComplete(String.format("Installed %s", download.getMapName()), 100);
  }
}
