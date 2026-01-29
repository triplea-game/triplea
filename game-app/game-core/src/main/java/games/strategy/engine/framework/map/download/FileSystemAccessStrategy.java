package games.strategy.engine.framework.map.download;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.triplea.http.client.maps.listing.MapDownloadItem;
import org.triplea.swing.SwingComponents;

@UtilityClass
class FileSystemAccessStrategy {

  void remove(
      final Component parentFrame,
      final Function<MapDownloadItem, Boolean> mapDeleteAction,
      final List<MapDownloadItem> toRemove,
      final Consumer<String> removeAction) {
    SwingComponents.promptUser(
        "Remove Maps?",
        "<html>Will remove "
            + toRemove.size()
            + " maps, are you sure? <br/>"
            + formatMapList(toRemove, MapDownloadItem::getMapName)
            + "</html>",
        newRemoveMapAction(parentFrame, mapDeleteAction, toRemove, removeAction));
  }

  private static Runnable newRemoveMapAction(
      final Component parentFrame,
      final Function<MapDownloadItem, Boolean> mapDeleteAction,
      final List<MapDownloadItem> maps,
      final Consumer<String> removeActionListeners) {
    return () -> {
      final List<MapDownloadItem> deletes = new ArrayList<>();

      // delete the map files
      for (final MapDownloadItem map : maps) {
        if (mapDeleteAction.apply(map)) {
          deletes.add(map);
        }
      }

      if (!deletes.isEmpty()) {
        deletes.stream().map(MapDownloadItem::getMapName).forEach(removeActionListeners);
        final String message = newDialogMessage("Successfully removed.", deletes);
        showDialog(parentFrame, message, deletes, MapDownloadItem::getMapName);
      }
    };
  }

  private static void showDialog(
      final Component parentFrame,
      final String message,
      final List<MapDownloadItem> mapList,
      final Function<MapDownloadItem, String> outputFunction) {

    SwingComponents.newMessageDialog(
        parentFrame,
        "<html>" + message + "<br /> " + formatMapList(mapList, outputFunction) + "</html>");
  }

  private static String newDialogMessage(
      final String message, final List<MapDownloadItem> mapList) {
    final String plural = mapList.size() != 1 ? "s" : "";
    return message + " " + mapList.size() + " map" + plural;
  }

  private static String formatMapList(
      final List<MapDownloadItem> mapList, final Function<MapDownloadItem, String> outputFunction) {
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
