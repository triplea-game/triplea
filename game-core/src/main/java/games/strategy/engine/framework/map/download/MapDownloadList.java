package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.triplea.util.Version;

class MapDownloadList {

  private final List<DownloadFileDescription> available = new ArrayList<>();
  private final List<DownloadFileDescription> installed = new ArrayList<>();
  private final List<DownloadFileDescription> outOfDate = new ArrayList<>();

  MapDownloadList(final Collection<DownloadFileDescription> downloads) {
    this(downloads, new FileSystemAccessStrategy());
  }

  @VisibleForTesting
  MapDownloadList(
      final Collection<DownloadFileDescription> downloads,
      final FileSystemAccessStrategy strategy) {
    for (final DownloadFileDescription download : downloads) {
      if (download == null) {
        return;
      }
      final Optional<Version> mapVersion = strategy.getMapVersion(download.getInstallLocation());

      if (mapVersion.isPresent()) {
        installed.add(download);
        if (download.getVersion() != null
            && download.getVersion().isGreaterThan(mapVersion.get())) {
          outOfDate.add(download);
        }
      } else {
        available.add(download);
      }
    }
  }

  @VisibleForTesting
  List<DownloadFileDescription> getAvailable() {
    return available;
  }

  List<DownloadFileDescription> getAvailableExcluding(
      final Collection<DownloadFileDescription> excluded) {
    return available.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }

  List<DownloadFileDescription> getInstalled() {
    return installed;
  }

  @VisibleForTesting
  List<DownloadFileDescription> getOutOfDate() {
    return outOfDate;
  }

  List<DownloadFileDescription> getOutOfDateExcluding(
      final Collection<DownloadFileDescription> excluded) {
    return outOfDate.stream().filter(not(excluded::contains)).collect(Collectors.toList());
  }
}
