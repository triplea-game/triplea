package org.triplea.http.client.maps.listing;

import feign.FeignException;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.HttpClient;
import org.triplea.http.client.lobby.AuthenticationHeaders;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
@Slf4j
public class MapsClient {
  public static final String MAPS_LISTING_PATH = "/maps/listing";
  public static final String MAPS_FOLDER_NAME = "downloadedMaps";
  private final MapsFeignClient mapsFeignClient;

  public MapsClient(final URI mapsServerUri) {
    mapsFeignClient = new HttpClient<>(MapsFeignClient.class, mapsServerUri).get();
  }

  public List<MapDownloadItem> fetchMapDownloads() {
    try {
      return mapsFeignClient.fetchMapListing(AuthenticationHeaders.systemIdHeaders());
    } catch (FeignException e) {
      log.warn(
          "Failed to download the list of available maps from TripleA servers.\n"
              + "You can download the needed maps manually into your TripleA maps subfolder from <a href='https://github.com/triplea-maps/'>https://github.com/triplea-maps/</a>.",
          e);
      return List.of();
    }
  }
}
