package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.InstalledMapsListing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class DownloadMapsWindowMapsListing {

  private final List<DownloadFileDescription> available = new ArrayList<>();
  private final List<DownloadFileDescription> installed = new ArrayList<>();
  private final List<DownloadFileDescription> outOfDate = new ArrayList<>();

  DownloadMapsWindowMapsListing(final Collection<DownloadFileDescription> downloads) {
    this(downloads, InstalledMapsListing.parseMapFiles());
  }

  @VisibleForTesting
  DownloadMapsWindowMapsListing(
      final Collection<DownloadFileDescription> downloads,
      final InstalledMapsListing installedMapsListing) {
    for (final DownloadFileDescription download : downloads) {
      if (download == null) {
        return;
      }

      if (!installedMapsListing.isMapInstalled(download.getMapName())) {
        available.add(download);
      } else {
        final int mapVersion = installedMapsListing.getMapVersionByName(download.getMapName());
        if (download.getVersion() != null && download.getVersion() > mapVersion) {
          outOfDate.add(download);
        } else {
          installed.add(download);
        }
      }
    }
  }

  List<DownloadFileDescription> getAvailableExcluding(
      final Collection<DownloadFileDescription> excluded) {
    return available.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }

  List<DownloadFileDescription> getOutOfDateExcluding(
      final Collection<DownloadFileDescription> excluded) {
    return outOfDate.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }
}
