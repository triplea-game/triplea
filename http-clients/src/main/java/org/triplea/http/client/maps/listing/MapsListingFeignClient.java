package org.triplea.http.client.maps.listing;

import feign.FeignException;
import feign.Headers;
import feign.RequestLine;
import java.util.List;
import org.triplea.http.client.HttpConstants;

@Headers({HttpConstants.CONTENT_TYPE_JSON, HttpConstants.ACCEPT_JSON})
public interface MapsListingFeignClient {

  /**
   * Creates a new issue on github.com.
   *
   * @throws FeignException Thrown on non-2xx responses.
   */
  @RequestLine("GET " + MapsListingClient.MAPS_LISTING_PATH)
  List<MapDownloadListing> fetchMapListing();
}
