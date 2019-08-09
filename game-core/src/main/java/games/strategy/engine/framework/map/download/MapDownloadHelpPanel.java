package games.strategy.engine.framework.map.download;

import games.strategy.engine.ClientFileSystemHelper;
import java.util.Arrays;
import java.util.stream.Collectors;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Simple panel that shows a list of map download help notes. */
public class MapDownloadHelpPanel extends JPanel {
  private static final long serialVersionUID = -7254964602067275442L;

  private static final String[] HELP_COMMENTS = {
    "Click the 'Available' tab to see maps available for download",
    "Select any map by clicking its title",
    "Hold control and/or shift to select multiples maps",
    "Map files will be installed to: "
        + ClientFileSystemHelper.getUserMapsFolder().getAbsolutePath(),
  };

  public MapDownloadHelpPanel() {
    final JLabel label = new JLabel(buildHelpHtmlOutput());
    add(label);
  }

  private static String buildHelpHtmlOutput() {
    return Arrays.stream(HELP_COMMENTS)
        .map(helpComment -> "<li>" + helpComment + "</li>")
        .collect(Collectors.joining("", "<html>", "</html>"));
  }
}
