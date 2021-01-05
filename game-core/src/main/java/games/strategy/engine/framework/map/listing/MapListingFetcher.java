package games.strategy.engine.framework.map.listing;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadRunnable;
import games.strategy.engine.framework.system.DevOverrides;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.live.servers.LiveServersFetcher;

/** Fetches the full listing of maps that are available for download. */
@UtilityClass
public class MapListingFetcher {

  /**
   * Parses a map list source for a list of maps available for download. Multiple sources for the
   * download list are checked, first any override locations followed by the live file that is on
   * remote server.
   */
  public static List<DownloadFileDescription> getMapDownloadList() {
    if (ClientSetting.useMapsServerBetaFeature.getValue().orElse(false)) {
      // Get the URI of the maps server (either from override or read it from the servers file) and
      // then send an API call to it requesting the list of maps available for download.
      return DevOverrides.readMapServerOverride()
          .or(() -> new LiveServersFetcher().getMapsServerUri())
          .map(MapsListingClient::new)
          .map(MapsListingClient::fetchMapDownloads)
          .map(MapListingFetcher::convertDownloadListings)
          .orElseGet(List::of);
    } else {
      return ClientSetting.mapListOverride
          .getValue()
          .map(DownloadRunnable::readLocalFile)
          .orElseGet(() -> DownloadRunnable.download(UrlConstants.MAP_DOWNLOAD_LIST));
    }
  }

  private static List<DownloadFileDescription> convertDownloadListings(
      final List<MapDownloadListing> downloadListings) {

    return downloadListings.stream()
        .map(DownloadFileDescription::ofMapDownloadListing)
        .collect(Collectors.toList());
  }
}
