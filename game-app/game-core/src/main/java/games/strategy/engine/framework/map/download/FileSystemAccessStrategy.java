package games.strategy.engine.framework.map.download;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadListing;
import org.triplea.swing.SwingComponents;

@UtilityClass
class FileSystemAccessStrategy {

  void remove(
      final Function<MapDownloadListing, Boolean> mapDeleteAction,
      final List<MapDownloadListing> toRemove,
      final Consumer<String> removeAction) {
    SwingComponents.promptUser(
        "Remove Maps?",
        "<html>Will remove "
            + toRemove.size()
            + " maps, are you sure? <br/>"
            + formatMapList(toRemove, MapDownloadListing::getMapName)
            + "</html>",
        newRemoveMapAction(mapDeleteAction, toRemove, removeAction));
  }

  private static Runnable newRemoveMapAction(
      final Function<MapDownloadListing, Boolean> mapDeleteAction,
      final List<MapDownloadListing> maps,
      final Consumer<String> removeActionListeners) {
    return () -> {
      final List<MapDownloadListing> deletes = new ArrayList<>();

      // delete the map files
      for (final MapDownloadListing map : maps) {
        if (mapDeleteAction.apply(map)) {
          deletes.add(map);
        }
      }

      if (!deletes.isEmpty()) {
        deletes.stream().map(MapDownloadListing::getMapName).forEach(removeActionListeners);
        final String message = newDialogMessage("Successfully removed.", deletes);
        showDialog(message, deletes, MapDownloadListing::getMapName);
      }
    };
  }

  private static void showDialog(
      final String message,
      final List<MapDownloadListing> mapList,
      final Function<MapDownloadListing, String> outputFunction) {

    SwingComponents.newMessageDialog(
        "<html>" + message + "<br /> " + formatMapList(mapList, outputFunction) + "</html>");
  }

  private static String newDialogMessage(
      final String message, final List<MapDownloadListing> mapList) {
    final String plural = mapList.size() != 1 ? "s" : "";
    return message + " " + mapList.size() + " map" + plural;
  }

  private static String formatMapList(
      final List<MapDownloadListing> mapList,
      final Function<MapDownloadListing, String> outputFunction) {
    final int maxMapsToList = 6;
    final StringBuilder sb = new StringBuilder("<ul>");
    for (int i = 0; i < mapList.size(); i++) {
      if (i > maxMapsToList) {
        sb.append("<li>...</li>");
        break;
      }
      sb.append("<li>").append(outputFunction.apply(mapList.get(i))).append("</li>");
    }
    return sb.toString();
  }
}
