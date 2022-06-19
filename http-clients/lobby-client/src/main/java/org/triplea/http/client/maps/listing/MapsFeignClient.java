package org.triplea.http.client.maps.listing;

import feign.FeignException;
import feign.RequestLine;
import java.util.List;

public interface MapsFeignClient {

  /**
   * Fetches a list of available maps from the server.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("GET " + MapsClient.MAPS_LISTING_PATH)
  List<MapDownloadItem> fetchMapListing();
}
