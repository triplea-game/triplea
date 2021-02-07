package games.strategy.engine.framework.map.download;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.DefaultListModel;
import lombok.experimental.UtilityClass;
import org.triplea.swing.SwingComponents;

@UtilityClass
class FileSystemAccessStrategy {

  static void remove(
      final List<DownloadFileDescription> toRemove, final DefaultListModel<String> listModel) {
    SwingComponents.promptUser(
        "Remove Maps?",
        "<html>Will remove "
            + toRemove.size()
            + " maps, are you sure? <br/>"
            + formatMapList(toRemove, DownloadFileDescription::getMapName)
            + "</html>",
        newRemoveMapAction(toRemove, listModel));
  }

  private static Runnable newRemoveMapAction(
      final List<DownloadFileDescription> maps, final DefaultListModel<String> listModel) {
    return () -> {
      final List<DownloadFileDescription> deletes = new ArrayList<>();

      // delete the map files
      for (final DownloadFileDescription map : maps) {
        if (map.delete()) {
          deletes.add(map);
        }
      }

      if (!deletes.isEmpty()) {
        deletes.stream().map(DownloadFileDescription::getMapName).forEach(listModel::removeElement);
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
