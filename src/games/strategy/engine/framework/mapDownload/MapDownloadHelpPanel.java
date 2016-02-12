package games.strategy.engine.framework.mapDownload;

import javax.swing.JLabel;
import javax.swing.JPanel;

import games.strategy.engine.ClientFileSystemHelper;

/** Simple panel that shows a list of map download help notes */
public class MapDownloadHelpPanel extends JPanel {
  private static final long serialVersionUID = -7254964602067275442L;

  private static final String[] HELP_COMMENTS = {
      "Install new maps by clicking the 'Available' tab, select a map by clicking it, and then click the 'Install' button.",
      "Hold shift or control while selecting maps to select multiple maps.",
      "While maps are downloading you may select additional maps and download them.",
      "Map will be installed to: " + ClientFileSystemHelper.getUserMapsFolder().getAbsolutePath(),
  };

  public MapDownloadHelpPanel() {

    JLabel label = new JLabel(buildHelpHtmlOutput());
    add(label);
  }

  private static String buildHelpHtmlOutput() {
    StringBuilder sb = new StringBuilder();
    sb.append("<html>");
    for (String helpComment : HELP_COMMENTS) {
      sb.append("<li>" + helpComment + "</li>");
    }
    sb.append("</html>");
    return sb.toString();
  }
}
