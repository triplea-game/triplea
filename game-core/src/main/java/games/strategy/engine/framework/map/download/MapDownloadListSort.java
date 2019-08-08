package games.strategy.engine.framework.map.download;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Utility class to sort a list of map downloads. */
public final class MapDownloadListSort {

  private MapDownloadListSort() {}

  /**
   * Sorts a list of map downloads alphabetically case insensitive by group where dummy URL headers
   * delimit the map groups.
   */
  // TODO: can be simplified now that we no longer have headers : )
  public static List<DownloadFileDescription> sortByMapName(
      final List<DownloadFileDescription> downloads) {
    checkNotNull(downloads);

    // Until we see a header, save each map to this List.
    // When we see a header, we'll sort this list, add it to the return values, and then clear it.
    final List<DownloadFileDescription> maps = new ArrayList<>(downloads);

    // in case the file does not end with a header, sort and add any remaining maps
    final List<DownloadFileDescription> returnList = new ArrayList<>();
    if (!maps.isEmpty()) {
      returnList.addAll(sort(maps));
    }
    return returnList;
  }

  private static List<DownloadFileDescription> sort(final List<DownloadFileDescription> maps) {
    maps.sort(Comparator.comparing(lhs -> lhs.getMapName().toUpperCase()));
    return maps;
  }
}
