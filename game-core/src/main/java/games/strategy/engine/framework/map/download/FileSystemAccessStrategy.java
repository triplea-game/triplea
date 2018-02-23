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
import games.strategy.util.Interruptibles;
import games.strategy.util.Version;

class FileSystemAccessStrategy {

  FileSystemAccessStrategy() {}

  Optional<Version> getMapVersion(final String mapName) {
    final File potentialFile = new File(mapName);

    if (!potentialFile.exists()) {
      return Optional.empty();
    }

    final DownloadFileProperties props = DownloadFileProperties.loadForZip(potentialFile);
    return (props.getVersion() == null) ? Optional.empty() : Optional.of(props.getVersion());
  }

  static void remove(final List<DownloadFileDescription> toRemove, final DefaultListModel<String> listModel) {
    SwingComponents.promptUser("Remove Maps?",
        "<html>Will remove " + toRemove.size() + " maps, are you sure? <br/>"
            + formatMapList(toRemove, map -> map.getMapName()) + "</html>",
        createRemoveMapAction(toRemove, listModel));
  }

  private static Runnable createRemoveMapAction(final List<DownloadFileDescription> maps,
      final DefaultListModel<String> listModel) {
    return () -> {

      // delete the map files
      for (final DownloadFileDescription map : maps) {
        try {
          Files.delete(map.getInstallLocation().toPath());
        } catch (final IOException e) {
          ClientLogger.logQuietly("Failed to delete map: " + map.getInstallLocation().getAbsolutePath(), e);
        }
        map.getInstallLocation().delete();
      }

      // now sleep a short while before we check our work
      Interruptibles.sleep(10);

      // check our work, see if we actuall deleted stuff
      final List<DownloadFileDescription> deletes = new ArrayList<>();
      final List<DownloadFileDescription> fails = new ArrayList<>();
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
        deletes.stream()
            .map(DownloadFileDescription::getInstallLocation)
            .map(location -> location + ".properties")
            .map(File::new)
            .forEach(File::delete);

        deletes.stream().map(DownloadFileDescription::getMapName).forEach(listModel::removeElement);
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

    SwingComponents.newMessageDialog(
        "<html>" + message + "<br /> " + formatMapList(mapList, outputFunction) + "</html>");
  }

  private static String createDialogMessage(final String message, final List<DownloadFileDescription> mapList) {
    final String plural = (mapList.size() != 1) ? "s" : "";
    return message + " " + mapList.size() + " map" + plural;
  }

  private static String formatMapList(final List<DownloadFileDescription> mapList,
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
