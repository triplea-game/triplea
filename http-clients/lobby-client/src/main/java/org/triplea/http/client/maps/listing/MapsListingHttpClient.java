package org.triplea.http.client.maps.listing;

import java.net.URI;
import java.util.List;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
public class MapsListingHttpClient implements MapsListingClient {
  private final MapsListingFeignClient mapsListingFeignClient;

  public MapsListingHttpClient(final URI mapsServerUri) {
    mapsListingFeignClient = new HttpClient<>(MapsListingFeignClient.class, mapsServerUri).get();
  }

  @Override
  public List<MapDownloadListing> fetchMapDownloads() {
    return mapsListingFeignClient.fetchMapListing(AuthenticationHeaders.systemIdHeaders());
  }
}
