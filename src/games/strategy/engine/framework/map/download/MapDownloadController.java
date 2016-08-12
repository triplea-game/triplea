package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.ui.background.BackgroundTaskRunner;
import games.strategy.triplea.settings.SystemPreferenceKey;
import games.strategy.triplea.settings.SystemPreferences;
import games.strategy.ui.SwingComponents;


/** Controller for in-game map download actions */
public class MapDownloadController {

  private final MapListingSource mapDownloadProperties;

  public MapDownloadController(final MapListingSource mapSource) {
    mapDownloadProperties = mapSource;
  }

  private DownloadRunnable downloadForLatestMapsCheck() {
    final DownloadRunnable runnable = new DownloadRunnable(mapDownloadProperties.getMapListDownloadSite());
    BackgroundTaskRunner.runInBackground("Checking for out-of-date Maps.", runnable);
    return runnable;
  }

  /**
   * Return true if all locally downloaded maps are latest versions, false if any can are out of date or their version
   * not recognized
   */
  public boolean checkDownloadedMapsAreLatest() {
    try {
      // check at most once per month
      final Calendar calendar = Calendar.getInstance();
      final int year = calendar.get(Calendar.YEAR);
      final int month = calendar.get(Calendar.MONTH);
      // format year:month
      final String lastCheckTime = SystemPreferences.get(SystemPreferenceKey.TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES, "");
      if (lastCheckTime != null && lastCheckTime.trim().length() > 0) {
        final String[] yearMonth = lastCheckTime.split(":");
        if (Integer.parseInt(yearMonth[0]) >= year && Integer.parseInt(yearMonth[1]) >= month) {
          return false;
        }
      }
      SystemPreferences.put(SystemPreferenceKey.TRIPLEA_LAST_CHECK_FOR_MAP_UPDATES, year + ":" + month);

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
        SwingComponents.promptUser("Update Your Maps?", text.toString(), DownloadMapsWindow::showDownloadMapsWindow);
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for map updates", e);
    }
    return false;
  }


  private static void populateOutOfDateMapsListing(final Collection<String> listingToBeAddedTo,
      final Collection<DownloadFileDescription> gamesDownloadFileDescriptions) {
    if (listingToBeAddedTo == null) {
      return;
    }
    listingToBeAddedTo.clear();
    for (final DownloadFileDescription d : gamesDownloadFileDescriptions) {
      if (d != null && !d.isDummyUrl()) {
        File installed = new File(ClientFileSystemHelper.getUserMapsFolder(), d.getMapName() + ".zip");
        if (!installed.exists()) {
          installed = new File(GameSelectorModel.DEFAULT_MAP_DIRECTORY, d.getMapName() + ".zip");
        }
        if (installed.exists()) {
          if (d.getVersion() != null && d.getVersion().isGreaterThan(DownloadMapsWindow.getVersion(installed), true)) {
            listingToBeAddedTo.add(d.getMapName());
          }
        }
      }
    }
  }
}
