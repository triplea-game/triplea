package games.strategy.engine.framework.map.download;

import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import games.strategy.util.Version;

public class MapDownloadList {

  private final List<DownloadFileDescription> available = Lists.newArrayList();
  private final List<DownloadFileDescription> installed = Lists.newArrayList();
  private final List<DownloadFileDescription> outOfDate = Lists.newArrayList();

  public MapDownloadList(final List<DownloadFileDescription> downloads, final FileSystemAccessStrategy strategy) {
    for (final DownloadFileDescription download : downloads) {
      if (download.isDummyUrl()) {
        available.add(download);
        installed.add(download);
      } else {
        final Optional<Version> mapVersion = strategy.getMapVersion(download.getInstallLocation().getAbsolutePath());

        if (mapVersion.isPresent()) {
          installed.add(download);
          if (download.getVersion().isGreaterThan(mapVersion.get())) {
            outOfDate.add(download);
          }
        } else {
          available.add(download);
        }
      }
    }
  }

  public List<DownloadFileDescription> getAvailable() {
    return available;
  }

  public List<DownloadFileDescription> getInstalled() {
    return installed;
  }

  public List<DownloadFileDescription> getOutOfDate() {
    return outOfDate;
  }
}
