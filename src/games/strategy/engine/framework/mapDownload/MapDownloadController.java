package games.strategy.engine.framework.mapDownload;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;


/** Controller for in-game map download actions */
public class MapDownloadController {

  private static final String TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES = "triplea.lastCheckForMapUpdates";
  private final MapListingSource mapDownloadProperties;

  public MapDownloadController(final MapListingSource mapSource) {
    mapDownloadProperties = mapSource;
  }

  /** Opens a new window dialog where a user can select maps to download or update */
  public void openDownloadMapScreen(JComponent parentComponent) {
    MapDownloadAction downloadAction = new MapDownloadAction(mapDownloadProperties);
    final DownloadRunnable download = downloadAction.downloadForAvailableMaps(parentComponent);

    if (download.getError() != null) {
      ClientLogger.logError(download.getError());
      return;
    }
    final Frame parentFrame = JOptionPane.getFrameForComponent(parentComponent);
    InstallMapDialog.installGames(parentFrame, download.getDownloads());
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

      MapDownloadAction downloadAction = new MapDownloadAction(mapDownloadProperties);
      final DownloadRunnable download = downloadAction.downloadForLatestMapsCheck();
      if (download.getError() != null) {
        return false;
      }
      final List<DownloadFileDescription> downloads = download.getDownloads();
      if (downloads == null || downloads.isEmpty()) {
        return false;
      }
      final List<String> outOfDateMaps = new ArrayList<String>();
      InstallMapDialog.populateOutOfDateMapsListing(outOfDateMaps, downloads);
      if (!outOfDateMaps.isEmpty()) {
        final StringBuilder text =
            new StringBuilder("<html>Some of the maps you have are out of date, and newer versions of those maps exist."
                + "<br>You should update (re-download) the following maps:<br><ul>");
        for (final String map : outOfDateMaps) {
          text.append("<li> " + map + "</li>");
        }
        text.append(
            "</ul><br><br>You can update them by clicking on the 'Download Maps' button on the start screen of TripleA.</html>");
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            EventThreadJOptionPane.showMessageDialog(null, text, "Update Your Maps", JOptionPane.INFORMATION_MESSAGE,
                false, new CountDownLatchHandler(true));
          }
        });
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for map updates", e);
    }
    return false;
  }
}
