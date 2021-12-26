package org.triplea.http.client.maps.listing;

import feign.FeignException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.AuthenticationHeaders;
import org.triplea.http.client.HttpClient;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
@Slf4j
public class MapsClient {
  public static final String MAPS_LISTING_PATH = "/maps/listing";
  private final MapsFeignClient mapsFeignClient;

  public MapsClient(final URI mapsServerUri) {
    mapsFeignClient = new HttpClient<>(MapsFeignClient.class, mapsServerUri).get();
  }

  public List<MapDownloadItem> fetchMapDownloads() {
    try {
      return mapsFeignClient.fetchMapListing(AuthenticationHeaders.systemIdHeaders());
    } catch (FeignException e) {
      log.error("Failed to download the list of available maps from TripleA servers.", e);
      return List.of();
    }
  }
}
