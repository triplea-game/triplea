package org.triplea.http.client.maps.listing;

import feign.FeignException;
import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.http.client.ClientIdentifiers;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.ServerPaths;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
public interface MapsClient {
  static MapsClient newClient(URI mapsServerUri, ClientIdentifiers clientIdentifiers) {
    return HttpClient.newClient(MapsClient.class, mapsServerUri, clientIdentifiers.createHeaders());
  }

  /**
   * Fetches a list of available maps from the server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("GET " + ServerPaths.MAPS_LISTING_PATH)
  List<org.triplea.http.client.lobby.maps.listing.MapDownloadItem> fetchMapListing();
}
