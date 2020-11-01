package games.strategy.engine.auto.update;

import games.strategy.triplea.UrlConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import lombok.experimental.UtilityClass;
import org.triplea.awt.OpenFileUtility;
import org.triplea.injection.Injections;
import org.triplea.util.Version;

@UtilityClass
class OutOfDateDialog {

  // TODO: METHOD-ORDERING re-order methods to depth-first ordering
  private static String getOutOfDateMessage(final Version latestVersionOut) {
    return "<html>"
        + "<h2>A new version of TripleA is out.  Please Update TripleA!</h2>"
        + "<br />Your current version: "
        + Injections.engineVersion()
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

  static Component showOutOfDateComponent(final Version latestVersionOut) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JEditorPane intro = new JEditorPane("text/html", getOutOfDateMessage(latestVersionOut));
    intro.setEditable(false);
    intro.setOpaque(false);
    intro.setBorder(BorderFactory.createEmptyBorder());
    final HyperlinkListener hyperlinkListener =
        e -> {
          if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            OpenFileUtility.openUrl(e.getDescription());
          }
        };
    intro.addHyperlinkListener(hyperlinkListener);
    panel.add(intro, BorderLayout.NORTH);
    final JEditorPane updates = new JEditorPane("text/html", getOutOfDateReleaseUpdates());
    updates.setEditable(false);
    updates.setOpaque(false);
    updates.setBorder(BorderFactory.createEmptyBorder());
    updates.addHyperlinkListener(hyperlinkListener);
    updates.setCaretPosition(0);
    final JScrollPane scroll = new JScrollPane(updates);
    panel.add(scroll, BorderLayout.CENTER);
    final Dimension maxDimension = panel.getPreferredSize();
    maxDimension.width = Math.min(maxDimension.width, 700);
    maxDimension.height = Math.min(maxDimension.height, 480);
    panel.setMaximumSize(maxDimension);
    panel.setPreferredSize(maxDimension);
    return panel;
  }
}
