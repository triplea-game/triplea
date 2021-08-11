package games.strategy.engine.framework.map.listing;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingHttpClient;
import org.triplea.live.servers.LiveServersFetcher;

@UtilityClass
public class MapListingFetcher {

  /** Fetches the full listing of maps that are available for download. */
  public static List<MapDownloadListing> getMapDownloadList() {
    final var serverUri = new LiveServersFetcher().serverForCurrentVersion().getUri();
    return new MapsListingHttpClient(serverUri).fetchMapDownloads();
  }
}
