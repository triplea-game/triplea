package games.strategy.engine.framework.map.download;

import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.ClientFileSystemHelper;

/** Simple panel that shows a list of map download help notes */
public class MapDownloadHelpPanel extends JPanel {
  private static final long serialVersionUID = -7254964602067275442L;

  private static final String[] HELP_COMMENTS = {
      "Click the 'Available' tab to see maps available for download",
      "Select any map by clicking its title",
      "Hold control and/or shift to select multiples maps",
      "Map files will be installed to: " + ClientFileSystemHelper.getUserMapsFolder().getAbsolutePath(),
  };

  public MapDownloadHelpPanel() {
    final JLabel label = new JLabel(buildHelpHtmlOutput());
    add(label);
  }

  private static String buildHelpHtmlOutput() {
    final StringBuilder sb = new StringBuilder();
    sb.append("<html>");
    for (final String helpComment : HELP_COMMENTS) {
      sb.append("<li>").append(helpComment).append("</li>");
    }
    sb.append("</html>");
    return sb.toString();
  }
}
