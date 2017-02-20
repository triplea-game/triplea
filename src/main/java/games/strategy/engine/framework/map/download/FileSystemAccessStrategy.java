package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.DefaultListModel;

import games.strategy.debug.ClientLogger;
import games.strategy.ui.SwingComponents;
import games.strategy.util.Version;

public class FileSystemAccessStrategy {

  public FileSystemAccessStrategy() {}

  public Optional<Version> getMapVersion(final String mapName) {
    final File potentialFile = new File(mapName);

    if (!potentialFile.exists()) {
      return Optional.empty();
    } else {
      final DownloadFileProperties props = DownloadFileProperties.loadForZip(potentialFile);
      if (props.getVersion() == null) {
        return Optional.empty();
      } else {
        return Optional.of(props.getVersion());
      }
    }
  }

  public static void remove(final List<DownloadFileDescription> toRemove, final DefaultListModel<String> listModel) {
    SwingComponents.promptUser("Remove Maps?",
        "<html>Will remove " + toRemove.size() + " maps, are you sure? <br/>"
            + formatMapList(toRemove, map -> map.getMapName()) + "</html>",
        createRemoveMapAction(toRemove, listModel));
  }

  private static Runnable createRemoveMapAction(final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    return () -> {
      final List<DownloadFileDescription> fails = new ArrayList<>();
      final List<DownloadFileDescription> deletes = new ArrayList<>();

      // delete the map files
      for (final DownloadFileDescription map : maps) {
        try {
          Files.delete(map.getInstallLocation().toPath());
        } catch (final IOException e) {
          ClientLogger.logQuietly(e);
        }
        map.getInstallLocation().delete();
      }

      // now sleep a short while before we check our work
      try {
        Thread.sleep(10);
      } catch (final InterruptedException e) {
      }

      // check our work, see if we actuall deleted stuff
      for (final DownloadFileDescription map : maps) {
        if (map.getInstallLocation().exists()) {
          fails.add(map);
        } else {
          deletes.add(map);
        }
      }


      if (!deletes.isEmpty()) {
        showRemoveSuccessDialog("Successfully removed.", deletes);
        // only once we know for sure we deleted things, then delete the ".properties" file
        deletes.forEach(dl -> (new File(dl.getInstallLocation() + ".properties")).delete());
        deletes.forEach(m -> listModel.removeElement(m.getMapName()));
      }

      if (!fails.isEmpty()) {
        showRemoveFailDialog("Unable to delete some of the maps files.<br />"
            + "Manual removal of the files may be necessary:", fails);
        fails.forEach(m -> m.getInstallLocation().deleteOnExit());
      }
    };
  }

  private static void showRemoveFailDialog(final String failMessage, final List<DownloadFileDescription> mapList) {
    final String message = createDialogMessage(failMessage, mapList);
    showDialog(message, mapList, (map) -> map.getInstallLocation().getAbsolutePath());
  }

  private static void showRemoveSuccessDialog(final String successMessage,
      final List<DownloadFileDescription> mapList) {
    final String message = createDialogMessage(successMessage, mapList);
    showDialog(message, mapList, (map) -> map.getMapName());
  }

  private static void showDialog(final String message, final List<DownloadFileDescription> mapList,
      final Function<DownloadFileDescription, String> outputFunction) {
    final StringBuilder sb = new StringBuilder("<html>" + message + "<br /> " + formatMapList(mapList, outputFunction));
    sb.append("</html>");

    SwingComponents.newMessageDialog(sb.toString());
  }

  private static String createDialogMessage(final String message, final List<DownloadFileDescription> mapList) {
    final String plural = mapList.size() > 0 ? "s" : "";
    return message + " " + mapList.size() + " map" + plural;
  }

  private static String formatMapList(final List<DownloadFileDescription> mapList,
      final Function<DownloadFileDescription, String> outputFunction) {
    final int MAX_MAPS_TO_LIST = 6;
    final StringBuilder sb = new StringBuilder("<ul>");
    for (int i = 0; i < mapList.size(); i++) {
      if (i > MAX_MAPS_TO_LIST) {
        sb.append("<li>...</li>");
        break;
      } else {
        sb.append("<li>").append(outputFunction.apply(mapList.get(i))).append("</li>");
      }
    }
    return sb.toString();
  }
}
