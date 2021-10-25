package games.strategy.engine.auto.update;

import games.strategy.triplea.UrlConstants;
import javax.swing.JOptionPane;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.swing.JEditorPaneWithClickableLinks;
import org.triplea.util.Version;

@UtilityClass
class OutOfDateDialog {
  static void showOutOfDateComponent(final Version latestVersionOut) {
    final int result =
        JOptionPane.showOptionDialog(
            null,
            new JEditorPaneWithClickableLinks(getOutOfDateMessage(latestVersionOut)),
            "TripleA is out of date!",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[] {"Download Now", "Remind Me Later"},
            null);

    if (result == JOptionPane.YES_OPTION) {
      OpenFileUtility.openUrl(UrlConstants.DOWNLOAD_WEBSITE);
    }
  }

  private static String getOutOfDateMessage(final Version latestVersionOut) {
    return String.format(
        "<html>"
            + "<h2>TripleA %s is available!</h2>"
            + "<center><a class=\"external\" href=\"%s\">Release Notes</a></center>"
            + "</html>",
        latestVersionOut, UrlConstants.RELEASE_NOTES);
  }
}
