package org.triplea.http.client.maps.listing;

import java.net.URI;
import java.util.List;
import org.triplea.http.client.HttpClient;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
public class MapsListingClient {
  public static final String MAPS_LISTING_PATH = "/maps/listing";
  private final MapsListingFeignClient mapsListingFeignClient;

  public MapsListingClient(final URI mapsServerUri) {
    mapsListingFeignClient = new HttpClient<>(MapsListingFeignClient.class, mapsServerUri).get();
  }

  public List<MapDownloadListing> fetchMapDownloads() {
    return mapsListingFeignClient.fetchMapListing();
  }
}
