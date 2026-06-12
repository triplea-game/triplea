package games.strategy.engine.framework.map.download;

import games.strategy.engine.framework.map.file.system.loader.InstalledMap;
import java.util.Objects;
import lombok.Getter;
import org.triplea.http.client.lobby.maps.listing.MapDownloadItem;

enum ManagedMapStatus {
  AVAILABLE,
  DOWNLOADING,
  INSTALLED,
  UPDATE_AVAILABLE,
  REMOVING,
  ERROR
}

/// Data model entity for individual map information
final class ManagedMap {
  @Getter private ManagedMapStatus mapStatus;
  @Getter private final MapDownloadItem mapDownloadItem;
  @Getter private InstalledMap installedMap = null; // already downloaded

  ManagedMap(final MapDownloadItem mapDownloadItem) {
    this.mapStatus = ManagedMapStatus.AVAILABLE;
    this.mapDownloadItem = Objects.requireNonNull(mapDownloadItem);
  }

  public ManagedMap(final MapDownloadItem mapDownloadItem, final InstalledMap installedMap) {
    this(mapDownloadItem);
    this.installedMap = Objects.requireNonNull(installedMap);
    this.mapStatus =
        installedMap.isOutOfDate(mapDownloadItem)
            ? ManagedMapStatus.UPDATE_AVAILABLE
            : ManagedMapStatus.INSTALLED;
  }

  String getMapName() {
    return mapDownloadItem.getMapName();
  }

  void setMapStatus(final ManagedMapStatus newStatus) {
    this.mapStatus = Objects.requireNonNull(newStatus);
  }

  @Override
  public String toString() {
    return getMapName() + " [" + mapStatus + "]";
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof ManagedMap that)) return false;
    return mapDownloadItem.getMapName().equals(that.mapDownloadItem.getMapName());
  }

  @Override
  public int hashCode() {
    // @todo currently the map name is assumed to be unique, solution is required, but the
    // limitation already exists today
    return mapDownloadItem.getMapName().hashCode();
  }
}
