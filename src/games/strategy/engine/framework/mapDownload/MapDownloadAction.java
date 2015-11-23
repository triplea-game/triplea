package games.strategy.engine.framework.mapDownload;

import javax.swing.JComponent;

import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.util.CountDownLatchHandler;

public class MapDownloadAction {

  private final MapDownloadProperties properties;

  public MapDownloadAction(final MapDownloadProperties properties) {
    this.properties = properties;
  }

  public DownloadRunnable downloadAvailableMapsInBackground() {
    final DownloadRunnable runnable = new DownloadRunnable(properties.getMapListDownloadSite(), true);
    BackgroundTaskRunner.runInBackground(null, "Checking for out-of-date Maps.", runnable,
        new CountDownLatchHandler(true));
    return runnable;
  }

  public DownloadRunnable downloadAvailableMapsInForeground(JComponent parentComponent) {
    final DownloadRunnable download = new DownloadRunnable(properties.getMapListDownloadSite(), true);
    // despite "BackgroundTaskRunner.runInBackground" saying runInBackground, it runs in a modal window in the
    // foreground.
    String popupWindowTitle = "Downloading list of availabe maps....";
    BackgroundTaskRunner.runInBackground(parentComponent.getRootPane(), popupWindowTitle, download);

    return download;
  }

}
