package org.triplea.maps.listing;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.http.client.maps.listing.MapDownloadItem;

@AllArgsConstructor
public class MapsListingModule implements Supplier<List<MapDownloadItem>> {

  private final MapListingDao mapListingDao;

  /**
   * Fetch all maps from database, for each fetch map tags from database, combine into a {@code
   * MapDownloadItem} and return a sorted list by map name.
   */
  @Override
  public List<MapDownloadItem> get() {
    return mapListingDao.fetchMapListings().stream()
        .map(this::databaseRecordToDownloadItemFunction)
        .sorted(Comparator.comparing(MapDownloadItem::getMapName))
        .collect(Collectors.toList());
  }

  private MapDownloadItem databaseRecordToDownloadItemFunction(
      final MapListingRecord mapListingRecord) {
    final var mapTagsFetchedFromDatabase =
        mapListingDao.fetchMapTagsForMapName(mapListingRecord.getName()).stream()
            .map(MapTagRecord::toMapTag)
            .collect(Collectors.toList());
    return mapListingRecord.toMapDownloadItem(mapTagsFetchedFromDatabase);
  }
}
