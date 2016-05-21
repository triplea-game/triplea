package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingComponents;
import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.util.CountDownLatchHandler;


/** Controller for in-game map download actions */
public class MapDownloadController {

  private static final String TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES = "triplea.lastCheckForMapUpdates";
  private final MapListingSource mapDownloadProperties;

  public MapDownloadController(final MapListingSource mapSource) {
    mapDownloadProperties = mapSource;
  }

  public DownloadRunnable downloadForLatestMapsCheck() {
    final DownloadRunnable runnable = new DownloadRunnable(mapDownloadProperties.getMapListDownloadSite());
    BackgroundTaskRunner.runInBackground(null, "Checking for out-of-date Maps.", runnable,
        new CountDownLatchHandler(true));
    return runnable;
  }

  public void downloadMap(String mapName) {
    DownloadMapsWindow.showDownloadMapsWindow(mapName);
  }


  /** Opens a new window dialog where a user can select maps to download or update */
  public void openDownloadMapScreen(JComponent parentComponent) {
    final JFrame parentFrame = (JFrame) SwingUtilities.getWindowAncestor(parentComponent);
    DownloadMapsWindow.showDownloadMapsWindow(parentFrame);
  }

  /**
   * Return true if all locally downloaded maps are latest versions, false if any can are out of date or their version
   * not recognized
   */
  public boolean checkDownloadedMapsAreLatest() {
    try {
      final Preferences pref = Preferences.userNodeForPackage(GameRunner2.class);
      // check at most once per month
      final Calendar calendar = Calendar.getInstance();
      final int year = calendar.get(Calendar.YEAR);
      final int month = calendar.get(Calendar.MONTH);
      // format year:month
      final String lastCheckTime = pref.get(TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES, "");
      if (lastCheckTime != null && lastCheckTime.trim().length() > 0) {
        final String[] yearMonth = lastCheckTime.split(":");
        if (Integer.parseInt(yearMonth[0]) >= year && Integer.parseInt(yearMonth[1]) >= month) {
          return false;
        }
      }
      pref.put(TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES, year + ":" + month);
      try {
        pref.sync();
      } catch (final BackingStoreException e) {
      }

      MapDownloadController controller = new MapDownloadController(mapDownloadProperties);
      final DownloadRunnable download = controller.downloadForLatestMapsCheck();
      if (download.getError() != null) {
        return false;
      }
      final List<DownloadFileDescription> downloads = download.getDownloads();
      if (downloads == null || downloads.isEmpty()) {
        return false;
      }
      final List<String> outOfDateMaps = new ArrayList<>();
      populateOutOfDateMapsListing(outOfDateMaps, downloads);
      if (!outOfDateMaps.isEmpty()) {
        final StringBuilder text =
            new StringBuilder("<html>Some of the maps you have are out of date, and newer versions of those maps exist."
                + "<br>Would you like to update (re-download) the following maps now?:<br><ul>");
        for (final String map : outOfDateMaps) {
          text.append("<li> ").append(map).append("</li>");
        }
        text.append("</ul></html>");
        SwingComponents.promptUser("Update Your Maps?", text.toString(), () -> DownloadMapsWindow.showDownloadMapsWindow());
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for map updates", e);
    }
    return false;
  }


  public static void populateOutOfDateMapsListing(final Collection<String> listingToBeAddedTo,
      final Collection<DownloadFileDescription> gamesDownloadFileDescriptions) {
    if (listingToBeAddedTo == null) {
      return;
    }
    listingToBeAddedTo.clear();
    for (final DownloadFileDescription d : gamesDownloadFileDescriptions) {
      if (d != null && !d.isDummyUrl()) {
        File installed = new File(ClientFileSystemHelper.getUserMapsFolder(), d.getMapName() + ".zip");
        if (installed == null || !installed.exists()) {
          installed = new File(GameSelectorModel.DEFAULT_MAP_DIRECTORY, d.getMapName() + ".zip");
        }
        if (installed != null && installed.exists()) {
          if (d.getVersion() != null && d.getVersion().isGreaterThan(DownloadMapsWindow.getVersion(installed), true)) {
            listingToBeAddedTo.add(d.getMapName());
          }
        }
      }
    }
  }
}
