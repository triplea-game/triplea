package org.triplea.http.client.maps.listing;

import java.util.List;

/**
 * Http client to communicate with the maps server and get a listing of maps available for download.
 */
public interface MapsListingClient {
  String MAPS_LISTING_PATH = "/maps/listing";

  List<MapDownloadListing> fetchMapDownloads();
}
