package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

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
  public static List<DownloadFileDescription> sortByMapName(final List<DownloadFileDescription> downloads) {
    checkNotNull(downloads);

    final List<DownloadFileDescription> returnList = Lists.newArrayList();

    // Until we see a header, save each map to this List.
    // When we see a header, we'll sort this list, add it
    // to the return values, and then clear it.
    List<DownloadFileDescription> maps = Lists.newArrayList();

    for (final DownloadFileDescription download : downloads) {
      if (download.isDummyUrl()) {
        returnList.addAll(sort(maps));
        maps = Lists.newArrayList();

        if (!returnList.isEmpty()) {
          // Add an empty row before any new headers (with exception to the first row)
          returnList.add(DownloadFileDescription.PLACE_HOLDER);
        }
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


  private static List<DownloadFileDescription> sort(final List<DownloadFileDescription> maps) {
    maps.sort((lhs, rhs) -> lhs.getMapName().toUpperCase().compareTo(rhs.getMapName().toUpperCase()));
    return maps;
  }
}
