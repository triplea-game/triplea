package tools.map.making.ui.runnable;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import tools.map.making.ui.MapEditorFrame;
import tools.util.FileOpen;
import tools.util.ToolArguments;

@Slf4j
abstract class MapEditorRunnableTask extends ToolRunnableTask {
  @Override
  protected void runInternal() throws IOException {
    log.info("Select the map");
    final Path mapFolderLocation = ToolArguments.getPropertyMapFolderPath().orElse(null);
    final Path mapName =
        new FileOpen(getParentComponent(), "Select The Map", mapFolderLocation, ".gif", ".png")
            .getFile();
    if (mapName != null) {
      log.info("Map : {}", mapName);
      final MapEditorFrame frame = getFrame(mapName);
      frame.setVisible(true);
      JOptionPane.showMessageDialog(frame, new JLabel(getWelcomeMessage()));
    } else {
      log.info("No Image Map Selected. Shutting down.");
    }
  }

  public abstract MapEditorFrame getFrame(Path mapPath) throws IOException;

  public abstract String getWelcomeMessage();
}
