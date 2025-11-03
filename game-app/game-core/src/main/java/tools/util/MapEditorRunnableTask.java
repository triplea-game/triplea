package tools.util;

import java.io.IOException;
import java.nio.file.Path;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import lombok.extern.slf4j.Slf4j;
import tools.image.FileOpen;
import tools.image.MapEditorFrame;

@Slf4j
public abstract class MapEditorRunnableTask extends ToolRunnableTask {
  @Override
  protected void runInternal() throws IOException {
    log.info("Select the map");
    final Path mapFolderLocation = ToolArguments.getPropertyMapFolderPath().orElse(null);
    final Path mapName =
        new FileOpen("Select The Map", mapFolderLocation, ".gif", ".png").getFile();
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
