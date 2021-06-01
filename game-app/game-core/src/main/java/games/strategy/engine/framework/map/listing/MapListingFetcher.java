package games.strategy.engine.framework.map.listing;

import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadRunnable;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.http.client.maps.listing.MapsListingClient;
import org.triplea.live.servers.LiveServersFetcher;
import org.triplea.live.servers.LiveServersFetcher.LobbyAddressFetchException;

/** Fetches the full listing of maps that are available for download. */
@Slf4j
public class MapListingFetcher {

  private final InstalledMapsListing installedMapsListing;

  private MapListingFetcher() {
    installedMapsListing = InstalledMapsListing.parseMapFiles();
  }

  /**
   * Parses a map list source for a list of maps available for download. Multiple sources for the
   * download list are checked, first any override locations followed by the live file that is on
   * remote server.
   */
  public static List<DownloadFileDescription> getMapDownloadList() {
    final MapListingFetcher mapListingFetcher = new MapListingFetcher();

    if (ClientSetting.useMapsServerBetaFeature.getValue().orElse(false)) {
      // Get the URI of the maps server (either from override or read it from the servers file) and
      // then send an API call to it requesting the list of maps available for download.
      try {
        final URI serverUri = new LiveServersFetcher().serverForCurrentVersion().getUri();
        final var downloads = new MapsListingClient(serverUri).fetchMapDownloads();
        return mapListingFetcher.convertDownloadListings(downloads);
      } catch (final LobbyAddressFetchException e) {
        log.warn(
            "Failed to download server properties. Check network connection. "
                + "Map listing will be empty. Error: "
                + e.getMessage(),
            e);
        return List.of();
      }
    } else {
      return ClientSetting.mapListOverride
          .getValue()
          .map(DownloadRunnable::readLocalFile)
          .orElseGet(() -> DownloadRunnable.download(UrlConstants.MAP_DOWNLOAD_LIST));
    }
  }

  private List<DownloadFileDescription> convertDownloadListings(
      final List<MapDownloadListing> downloadListings) {

    return downloadListings.stream()
        .map(map -> DownloadFileDescription.ofMapDownloadListing(map, installedMapsListing))
        .collect(Collectors.toList());
  }
}
