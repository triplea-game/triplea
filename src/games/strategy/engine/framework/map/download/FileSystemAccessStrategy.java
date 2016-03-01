package games.strategy.engine.framework.map.download;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

  public static void remove(List<DownloadFileDescription> toRemove) {
    SwingComponents.promptUser("Remove Maps?",
        "<html>Will remove " + toRemove.size() + " maps, are you sure? <br/>" + formatMapList(toRemove, map-> map.getMapName()) + "</html>",
        createRemoveMapAction(toRemove));
  }

  private static Runnable createRemoveMapAction(List<DownloadFileDescription> maps) {
    return () -> {
      List<DownloadFileDescription> fails = Lists.newArrayList();
      List<DownloadFileDescription> deletes = Lists.newArrayList();

      // delete the map files
      for (DownloadFileDescription map : maps) {
        map.getInstallLocation().delete();
      }

      // now sleep a short while before we check our work
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
      }

      // check our work, see if we actuall deleted stuff
      for (DownloadFileDescription map : maps) {
        if (map.getInstallLocation().exists()) {
          fails.add(map);
        } else {
          deletes.add(map);
        }
      }


      if (!deletes.isEmpty()) {
        showSuccessDialog("Successfully removed", deletes);
        // only once we know for sure we deleted things, then delete the ".properties" file
        deletes.forEach( dl -> (new File(dl.getInstallLocation() + ".properties")).delete());
      }

      if (!fails.isEmpty()) {
        showFailDialog("Unable to delete some maps files.\nPlease restart TripleA and check if the files have been removed.\n"
            + "If not, they will need to be removed manually:", fails);
        fails.forEach(m-> m.getInstallLocation().deleteOnExit());
      }
    };
  }

  private static void showFailDialog(String message, List<DownloadFileDescription> mapList) {
    showDialog(message,mapList, (map) -> map.getInstallLocation().getAbsolutePath());
  }

  private static void showSuccessDialog( String message, List<DownloadFileDescription> mapList) {
    showDialog(message,mapList, (map) -> map.getMapName());
  }

  private static void showDialog(String message, List<DownloadFileDescription> mapList,  Function<DownloadFileDescription,String> outputFunction) {
    String plural = mapList.size() > 0 ? "s" : "";
    SwingComponents.newMessageDialog(
        "<html>" + message + " " + mapList.size() + " map" + plural + "<br /> " + formatMapList(mapList, outputFunction)+ "</html>");
  }

  private static String formatMapList(List<DownloadFileDescription> mapList, Function<DownloadFileDescription,String> outputFunction) {
    final int MAX_MAPS_TO_LIST = 6;
    StringBuilder sb = new StringBuilder("<ul>");
    for (int i = 0; i < mapList.size(); i++) {
      if (i > MAX_MAPS_TO_LIST) {
        sb.append("<li>...</li>");
        break;
      } else {
        sb.append("<li>" + outputFunction.apply(mapList.get(i)) + "</li>");
      }
    }
    return sb.toString();
  }
}
