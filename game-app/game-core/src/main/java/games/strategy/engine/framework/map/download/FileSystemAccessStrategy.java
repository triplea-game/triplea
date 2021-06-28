package games.strategy.engine.framework.map.download;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingComponents;

@UtilityClass
class FileSystemAccessStrategy {

  void remove(
      final Function<DownloadFileDescription, Boolean> mapDeleteAction,
      final List<DownloadFileDescription> toRemove,
      final Consumer<String> removeAction) {
    SwingComponents.promptUser(
        "Remove Maps?",
        "<html>Will remove "
            + toRemove.size()
            + " maps, are you sure? <br/>"
            + formatMapList(toRemove, DownloadFileDescription::getMapName)
            + "</html>",
        newRemoveMapAction(mapDeleteAction, toRemove, removeAction));
  }

  private static Runnable newRemoveMapAction(
      final Function<DownloadFileDescription, Boolean> mapDeleteAction,
      final List<DownloadFileDescription> maps,
      final Consumer<String> removeActionListeners) {
    return () -> {
      final List<DownloadFileDescription> deletes = new ArrayList<>();

      // delete the map files
      for (final DownloadFileDescription map : maps) {
        if (mapDeleteAction.apply(map)) {
          deletes.add(map);
        }
      }

      if (!deletes.isEmpty()) {
        deletes.stream().map(DownloadFileDescription::getMapName).forEach(removeActionListeners);
        final String message = newDialogMessage("Successfully removed.", deletes);
        showDialog(message, deletes, DownloadFileDescription::getMapName);
      }
    };
  }

  private static void showDialog(
      final String message,
      final List<DownloadFileDescription> mapList,
      final Function<DownloadFileDescription, String> outputFunction) {

    SwingComponents.newMessageDialog(
        "<html>" + message + "<br /> " + formatMapList(mapList, outputFunction) + "</html>");
  }

  private static String newDialogMessage(
      final String message, final List<DownloadFileDescription> mapList) {
    final String plural = mapList.size() != 1 ? "s" : "";
    return message + " " + mapList.size() + " map" + plural;
  }

  private static String formatMapList(
      final List<DownloadFileDescription> mapList,
      final Function<DownloadFileDescription, String> outputFunction) {
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
