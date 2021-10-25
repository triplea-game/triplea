package games.strategy.engine.auto.update;

import games.strategy.triplea.UrlConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.experimental.UtilityClass;
import org.triplea.swing.EventThreadJOptionPane;
import org.triplea.swing.JEditorPaneWithClickableLinks;
import org.triplea.swing.SwingComponents;
import org.triplea.util.Version;

@UtilityClass
class OutOfDateDialog {
  static void showOutOfDateComponent(final Version latestVersionOut, final Version currentVersion) {
    SwingUtilities.invokeLater(
        () ->
            EventThreadJOptionPane.showMessageDialog(
                null,
                buildComponent(latestVersionOut, currentVersion),
                "TripleA is out of date!",
                JOptionPane.INFORMATION_MESSAGE));
  }

  static Component buildComponent(final Version latestVersionOut, final Version currentVersion) {
    final JPanel panel = new JPanel(new BorderLayout());

    final JEditorPane intro =
        new JEditorPaneWithClickableLinks(getOutOfDateMessage(latestVersionOut, currentVersion));

    panel.add(intro, BorderLayout.NORTH);

    final JEditorPane updates = new JEditorPaneWithClickableLinks(getOutOfDateReleaseUpdates());
    panel.add(SwingComponents.newJScrollPane(updates));
    return panel;
  }

  private static String getOutOfDateMessage(
      final Version latestVersionOut, final Version currentVersion) {
    return "<html>"
        + "<h2>A new version of TripleA is out.  Please Update TripleA!</h2>"
        + "<br />Your current version: "
        + currentVersion
        + "<br />Latest version available for download: "
        + latestVersionOut
        + "<br /><br />Click to download: <a class=\"external\" href=\""
        + UrlConstants.DOWNLOAD_WEBSITE
        + "\">"
        + UrlConstants.DOWNLOAD_WEBSITE
        + "</a>"
        + "</html>";
  }

  private static String getOutOfDateReleaseUpdates() {
    return "<html><body>"
        + "Link to full Change Log:<br /><a class=\"external\" href=\""
        + UrlConstants.RELEASE_NOTES
        + "\">"
        + UrlConstants.RELEASE_NOTES
        + "</a><br />"
        + "</body></html>";
  }
}
