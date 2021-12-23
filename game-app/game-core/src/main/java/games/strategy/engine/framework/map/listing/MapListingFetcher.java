package games.strategy.engine.framework.map.listing;

import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.http.client.maps.listing.MapsClient;

@UtilityClass
public class MapListingFetcher {

  /** Fetches the full listing of maps that are available for download. */
  public static List<MapDownloadItem> getMapDownloadList() {
    final var serverUri = ClientSetting.lobbyUri.getValueOrThrow();
    return new MapsClient(serverUri).fetchMapDownloads();
  }
}
