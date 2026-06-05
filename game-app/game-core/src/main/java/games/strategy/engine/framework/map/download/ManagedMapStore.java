package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

enum StoreState {
  UNINITIALIZED,
  INITIALIZING,
  INITIALIZED
}

final class ManagedMapStore {

  private final Map<String, ManagedMap> mapsByName = new HashMap<>();
  private final Map<ManagedMapStatus, Set<ManagedMap>> groupsByStatus =
      new EnumMap<>(ManagedMapStatus.class);
  @Getter private StoreState storeState;

  ManagedMapStore() {
    storeState = StoreState.UNINITIALIZED;
  }

  public void initialize(
      Collection<MapDownloadItem> downloads, InstalledMapsListing installedMapsListing) {
    storeState = StoreState.INITIALIZING;
    for (ManagedMapStatus mapStatus : ManagedMapStatus.values()) {
      groupsByStatus.put(mapStatus, new HashSet<>());
    }
    downloads.forEach(
        downloadItem -> {
          Optional<InstalledMap> installedMapByName =
              installedMapsListing.findInstalledMapByName(downloadItem.getMapName());
          put(
              installedMapByName
                  .map(installedMap -> new ManagedMap(downloadItem, installedMap))
                  .orElseGet(() -> new ManagedMap(downloadItem)));
        });
    storeState = StoreState.INITIALIZED;
  }

  ManagedMap get(final String mapName) {
    return mapsByName.get(mapName);
  }

  boolean contains(final String mapName) {
    return mapsByName.containsKey(mapName);
  }

  Collection<ManagedMap> getAll() {
    return Collections.unmodifiableCollection(mapsByName.values());
  }

  List<ManagedMap> getAllSortedByName() {
    final List<ManagedMap> result = new ArrayList<>(mapsByName.values());
    result.sort(Comparator.comparing(ManagedMap::getMapName));
    return result;
  }

  void put(final ManagedMap map) {
    mapsByName.put(map.getMapName(), map);
    groupsByStatus.get(map.getMapStatus()).add(map);
  }

  void remove(final String mapName) {
    mapsByName.remove(mapName);
  }

  void clear() {
    mapsByName.clear();
  }

  void updateStatus(final String mapName, final ManagedMapStatus status) {
    final ManagedMap map = mapsByName.get(mapName);
    if (map != null) {
      map.setMapStatus(status);
    }
  }

  List<ManagedMap> getByStatus(final ManagedMapStatus status) {
    return mapsByName.values().stream()
        .filter(m -> m.getMapStatus() == status)
        .sorted(Comparator.comparing(ManagedMap::getMapName))
        .toList();
  }

  int getCountByStatus(final ManagedMapStatus status) {
    return groupsByStatus.get(status).size();
  }

  int size() {
    return mapsByName.size();
  }
}
