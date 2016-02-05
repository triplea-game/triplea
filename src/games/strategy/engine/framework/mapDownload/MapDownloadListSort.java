package games.strategy.engine.framework.mapDownload;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * Utility class to sort a list of map downloads.
 */
public final class MapDownloadListSort {

  private MapDownloadListSort() {}

  /**
   * Sorts a list of map downloads alphabetically case insensitive by group where dummy URL headers delimit the map
   * groups.
   */
  public static List<DownloadFileDescription> sortByMapName(List<DownloadFileDescription> downloads) {
    checkState(!checkNotNull(downloads).isEmpty());

    List<DownloadFileDescription> returnList = Lists.newArrayList();

    // Until we see a header, save each map to this List.
    // When we see a header, we'll sort this list, add it
    // to the return values, and then clear it.
    List<DownloadFileDescription> maps = Lists.newArrayList();

    for (DownloadFileDescription download : downloads) {
      if (download.isDummyUrl()) {
        returnList.addAll(sort(maps));
        maps = Lists.newArrayList();
        returnList.add(download);
      } else {
        maps.add(download);
      }
    }

    // in case the file does not end with a header, sort and add any remaining maps
    if (!maps.isEmpty()) {
      returnList.addAll(sort(maps));
    }
    return returnList;
  }


  private static List<DownloadFileDescription> sort(List<DownloadFileDescription> maps) {
    maps.sort((lhs, rhs) -> lhs.getMapName().toUpperCase().compareTo(rhs.getMapName().toUpperCase()));
    return maps;
  }
}
