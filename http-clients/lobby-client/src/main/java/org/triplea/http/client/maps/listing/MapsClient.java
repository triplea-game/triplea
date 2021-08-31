package org.triplea.http.client.maps.listing;

import java.net.URI;
import java.util.List;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
public class MapsClient {
  private final MapsFeignClient mapsFeignClient;
  public static final String MAPS_LISTING_PATH = "/maps/listing";

  public MapsClient(final URI mapsServerUri) {
    mapsFeignClient = new HttpClient<>(MapsFeignClient.class, mapsServerUri).get();
  }

  public List<MapDownloadItem> fetchMapDownloads() {
    return mapsFeignClient.fetchMapListing(AuthenticationHeaders.systemIdHeaders());
  }
}
