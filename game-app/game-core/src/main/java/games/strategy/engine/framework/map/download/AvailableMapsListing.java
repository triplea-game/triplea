package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.DownloadedMapsListing;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class AvailableMapsListing {

  private final List<DownloadFileDescription> available = new ArrayList<>();
  private final List<DownloadFileDescription> installed = new ArrayList<>();
  private final List<DownloadFileDescription> outOfDate = new ArrayList<>();

  AvailableMapsListing(final Collection<DownloadFileDescription> downloads) {
    this(downloads, DownloadedMapsListing.parseMapFiles());
  }

  @VisibleForTesting
  AvailableMapsListing(
      final Collection<DownloadFileDescription> downloads,
      final DownloadedMapsListing downloadedMapsListing) {
    for (final DownloadFileDescription download : downloads) {
      if (download == null) {
        return;
      }

      if (!downloadedMapsListing.isMapInstalled(download.getMapName())) {
        available.add(download);
      } else {
        final int mapVersion = downloadedMapsListing.getMapVersionByName(download.getMapName());
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
