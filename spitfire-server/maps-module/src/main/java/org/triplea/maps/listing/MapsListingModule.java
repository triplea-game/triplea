package org.triplea.maps.listing;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.jdbi.v3.core.Jdbi;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.http.client.maps.listing.MapTag;
import org.triplea.maps.tags.MapTagsDao;

@AllArgsConstructor
public class MapsListingModule implements Supplier<List<MapDownloadItem>> {

  private final MapListingDao mapListingDao;
  private final MapTagsDao mapTagsDao;

  public static MapsListingModule build(final Jdbi jdbi) {
    return new MapsListingModule(
        jdbi.onDemand(MapListingDao.class), //
        new MapTagsDao(jdbi));
  }

  /** Returns data for the full set of maps available to download. */
  @Override
  public List<MapDownloadItem> get() {
    final Collection<MapListingRecord> mapListingRecords = mapListingDao.fetchMapListings();
    final Collection<MapTagRecord> mapTags = mapTagsDao.fetchAllMapTags();

    // iterate over each map listing and combine it with the corresponding map tags
    return mapListingRecords.stream()
        .map(mapListingRecord -> toMapDownloadItem(mapListingRecord, mapTags))
        .collect(Collectors.toList());
  }

  private MapDownloadItem toMapDownloadItem(
      final MapListingRecord mapListingRecord, final Collection<MapTagRecord> mapTags) {

    final List<MapTag> mapTagsForThisMap =
        mapTags.stream()
            .filter(m -> m.getMapName().equals(mapListingRecord.getName()))
            .map(MapTagRecord::toMapTag)
            .collect(Collectors.toList());

    return mapListingRecord.toMapDownloadItem(mapTagsForThisMap);
  }
}
