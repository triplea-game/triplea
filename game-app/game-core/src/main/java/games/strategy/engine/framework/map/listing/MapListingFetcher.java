package games.strategy.engine.framework.map.listing;

import games.strategy.engine.framework.map.download.DownloadRunnable;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.http.client.maps.listing.MapsListingHttpClient;
import org.triplea.live.servers.LiveServersFetcher;

@UtilityClass
public class MapListingFetcher {

  /** Fetches the full listing of maps that are available for download. */
  public static List<MapDownloadListing> getMapDownloadList() {
    final MapsListingClient mapsListingClient =
        ClientSetting.useMapsServerBetaFeature.getValue().orElse(false)
            ? new MapsListingHttpClient(new LiveServersFetcher().serverForCurrentVersion().getUri())
            : new DownloadRunnable(UrlConstants.MAP_DOWNLOAD_LIST);

    return mapsListingClient.fetchMapDownloads();
  }
}
