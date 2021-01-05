package org.triplea.maps.listing;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.http.client.maps.listing.MapDownloadListing;

@AllArgsConstructor
class MapsListingModule implements Supplier<List<MapDownloadListing>> {

  private final MapListingDao mapListingDao;

  @Override
  public List<MapDownloadListing> get() {
    return mapListingDao.fetchMapListings().stream()
        .map(MapListingRecord::toMapDownloadListing)
        .collect(Collectors.toList());
  }
}
