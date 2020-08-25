package org.triplea.maps.listing;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.triplea.http.client.maps.listing.MapDownloadListing;

@AllArgsConstructor
class MapsListingModule implements Supplier<List<MapDownloadListing>> {

  private final MapListingDao mapListingDao;
  private final MapSkinsListingDao mapSkinsListingDao;

  @Override
  public List<MapDownloadListing> get() {
    // map the skins by mapId -> skin
    final Multimap<Integer, MapSkinRecord> mapSkins = HashMultimap.create();
    mapSkinsListingDao.fetchMapSkinsListings().forEach(skin -> mapSkins.put(skin.getMapId(), skin));

    return mapListingDao.fetchMapListings().stream()
        .map(
            map ->
                map.toMapDownloadListing(
                    Optional.ofNullable(mapSkins.get(map.getId())).orElse(List.of())))
        .collect(Collectors.toList());
  }
}
