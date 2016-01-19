package games.strategy.engine.framework.mapDownload;

import javax.swing.JComponent;

import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.util.CountDownLatchHandler;

/**
 * Wraps a MapDownloadProperties object and allows us to launch these map related actions:
 * - check if maps are up to date
 * - get the list of available maps
 */
public class MapDownloadAction {

  private final MapListingSource mapListingSource;

  public MapDownloadAction(final MapListingSource properties) {
    this.mapListingSource = properties;
  }

  public DownloadRunnable downloadForLatestMapsCheck() {
    final DownloadRunnable runnable = new DownloadRunnable(mapListingSource.getMapListDownloadSite(), true);
    BackgroundTaskRunner.runInBackground(null, "Checking for out-of-date Maps.", runnable,
        new CountDownLatchHandler(true));
    return runnable;
  }

  public DownloadRunnable downloadForAvailableMaps(JComponent parentComponent) {
    final DownloadRunnable download = new DownloadRunnable(mapListingSource.getMapListDownloadSite(), true);
    // despite "BackgroundTaskRunner.runInBackground" saying runInBackground, it runs in a modal window in the
    // foreground.
    String popupWindowTitle = "Downloading list of availabe maps....";
    BackgroundTaskRunner.runInBackground(parentComponent.getRootPane(), popupWindowTitle, download);

    return download;
  }
}
