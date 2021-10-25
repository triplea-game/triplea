package games.strategy.engine.auto.update;

import games.strategy.triplea.UrlConstants;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.JEditorPaneWithClickableLinks;
import org.triplea.util.Version;

@UtilityClass
class OutOfDateDialog {
  static void showOutOfDateComponent(final Version latestVersionOut) {
    SwingUtilities.invokeLater(
        () ->
            EventThreadJOptionPane.showMessageDialog(
                null,
                new JEditorPaneWithClickableLinks(getOutOfDateMessage(latestVersionOut)),
                "Update TripleA!",
                JOptionPane.INFORMATION_MESSAGE));
  }

  private static String getOutOfDateMessage(final Version latestVersionOut) {
    return String.format(
        "<html>"
            + "<h2>TripleA %s is available!</h2>"
            + "<br><br>Click to download: <a class=\"external\" href=\"%s\">%s</a>"
            + "</a>"
            + "<br><br>"
            + "Release notes:<br /><a class=\"external\" href=\"%s\">%s</a><br />"
            + "</html>",
        latestVersionOut,
        UrlConstants.DOWNLOAD_WEBSITE,
        UrlConstants.DOWNLOAD_WEBSITE,
        UrlConstants.RELEASE_NOTES,
        UrlConstants.RELEASE_NOTES);
  }
}
