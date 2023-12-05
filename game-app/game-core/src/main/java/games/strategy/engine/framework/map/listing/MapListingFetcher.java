package games.strategy.engine.framework.map.listing;

import feign.FeignException;
import games.strategy.triplea.settings.ClientSetting;
import java.util.List;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.domain.data.SystemIdLoader;
import org.triplea.http.client.lib.ClientIdentifiers;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.http.client.maps.listing.MapsClient;

@UtilityClass
@Slf4j
public class MapListingFetcher {

  /** Fetches the full listing of maps that are available for download. */
  public static List<MapDownloadItem> getMapDownloadList() {
    final var serverUri = ClientSetting.lobbyUri.getValueOrThrow();
    try {
      return MapsClient.newClient(
              serverUri,
              ClientIdentifiers.builder()
                  .applicationVersion(ProductVersionReader.getCurrentVersion().toMajorMinorString())
                  .systemId(SystemIdLoader.load().getValue())
                  .build())
          .fetchMapListing();
    } catch (FeignException e) {
      log.warn(
          "Failed to download the list of available maps from TripleA servers.\n"
              + "You can download the needed maps manually into your TripleA maps subfolder from <a href='https://github.com/triplea-maps/'>https://github.com/triplea-maps/</a>.",
          e);
      return List.of();
    }
  }
}
