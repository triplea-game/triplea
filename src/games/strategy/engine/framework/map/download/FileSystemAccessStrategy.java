package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;

import games.strategy.common.swing.SwingComponents;
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

  public static void remove(List<DownloadFileDescription> toRemove, Runnable removeCompleteCallback) {
    SwingComponents.promptUser("Remove Maps?",
        "<html>Will remove " + toRemove.size() + " maps, are you sure? <br/>" + formatMapList(toRemove) + "</html>",
        createRemoveMapAction(toRemove, removeCompleteCallback));
  }

  private static Runnable createRemoveMapAction(List<DownloadFileDescription> maps,
      Runnable removeCompleteCallback) {
    return () -> {
      List<DownloadFileDescription> fails = Lists.newArrayList();
      List<DownloadFileDescription> deletes = Lists.newArrayList();

      for (DownloadFileDescription map : maps) {
        map.getInstallLocation().delete();
        (new File(map.getInstallLocation() + ".properties")).delete();

        if (map.getInstallLocation().exists()) {
          fails.add(map);
        } else {
          deletes.add(map);
        }
      }

      if (!deletes.isEmpty()) {
        showDialog("Successfully removed", deletes);
      }
      if (!fails.isEmpty()) {
        showDialog("Failed to remove", fails);
      }
      removeCompleteCallback.run();
    };
  }

  private static void showDialog(String message, List<DownloadFileDescription> mapList) {
    String plural = mapList.size() > 0 ? "s" : "";
    SwingComponents.newMessageDialog(
        "<html>" + message + " " + mapList.size() + " map" + plural + "<br /> " + formatMapList(mapList) + "</html>");
  }

  private static String formatMapList(List<DownloadFileDescription> mapList) {
    final int MAX_MAPS_TO_LIST = 6;
    StringBuilder sb = new StringBuilder("<ul>");
    for (int i = 0; i < mapList.size(); i++) {
      if (i > MAX_MAPS_TO_LIST) {
        sb.append("<li>...</li>");
        break;
      } else {
        sb.append("<li>" + mapList.get(i).getMapName() + "</li>");
      }
    }
    return sb.toString();
  }
}
