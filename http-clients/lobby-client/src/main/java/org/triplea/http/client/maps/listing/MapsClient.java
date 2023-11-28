package org.triplea.http.client.maps.listing;

import feign.FeignException;
import feign.RequestLine;
import java.net.URI;
import java.util.List;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
public interface MapsClient {
  String MAPS_LISTING_PATH = "/maps/listing";

  static MapsClient newClient(URI mapsServerUri) {
    return HttpClient.newClient(
        MapsClient.class, mapsServerUri, AuthenticationHeaders.systemIdHeaders());
  }

  /**
   * Fetches a list of available maps from the server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("GET " + MapsClient.MAPS_LISTING_PATH)
  List<MapDownloadItem> fetchMapListing();
}
