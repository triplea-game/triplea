package games.strategy.engine.framework.map.download;

import static java.util.function.Predicate.not;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.map.file.system.loader.DownloadedMaps;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;

@Getter
class MapDownloadList {

  private final List<DownloadFileDescription> available = new ArrayList<>();
  private final List<DownloadFileDescription> installed = new ArrayList<>();
  private final List<DownloadFileDescription> outOfDate = new ArrayList<>();

  MapDownloadList(final Collection<DownloadFileDescription> downloads) {
    this(downloads, DownloadedMaps.parseMapFiles());
  }

  @VisibleForTesting
  MapDownloadList(
      final Collection<DownloadFileDescription> downloads, final DownloadedMaps downloadedMaps) {
    for (final DownloadFileDescription download : downloads) {
      if (download == null) {
        return;
      }

      if (download.getInstallLocation().isPresent()) {
        final Optional<Integer> mapVersion =
            downloadedMaps.getMapVersionByName(download.getMapName());
        if (download.getVersion() != null
            && (mapVersion.isEmpty() || download.getVersion() > mapVersion.get())) {
          outOfDate.add(download);
        } else {
          installed.add(download);
        }
      } else {
        available.add(download);
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
