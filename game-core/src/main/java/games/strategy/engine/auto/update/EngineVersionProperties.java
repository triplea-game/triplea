package games.strategy.engine.auto.update;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import games.strategy.engine.ClientContext;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.Version;
import lombok.extern.java.Log;

@Log
class EngineVersionProperties {
  private static final String TRIPLEA_VERSION_LINK =
      "https://raw.githubusercontent.com/triplea-game/triplea/master/latest_version.properties";
  private final Version latestVersionOut;
  private final String link;
  private final String changelogLink;

  EngineVersionProperties() {
    this(getProperties());
  }

  private EngineVersionProperties(final Properties props) {
    latestVersionOut =
        new Version(props.getProperty("LATEST", ClientContext.engineVersion().toStringFull()));
    link = props.getProperty("LINK", UrlConstants.DOWNLOAD_WEBSITE.toString());
    changelogLink = props.getProperty("CHANGELOG", UrlConstants.RELEASE_NOTES.toString());
  }


  private static Properties getProperties() {
    final Properties props = new Properties();
    try {
      props.load(new URL(TRIPLEA_VERSION_LINK).openStream());
    } catch (final IOException e) {
      log.log(Level.SEVERE, "Failed while attempting to check for a new Version", e);
    }
    return props;
  }

  public Version getLatestVersionOut() {
    return latestVersionOut;
  }

  private String getOutOfDateMessage() {
    return "<html>" + "<h2>A new version of TripleA is out.  Please Update TripleA!</h2>"
        + "<br />Your current version: " + ClientContext.engineVersion().getExactVersion()
        + "<br />Latest version available for download: " + getLatestVersionOut()
        + "<br /><br />Click to download: <a class=\"external\" href=\"" + link
        + "\">" + link + "</a>"
        + "</html>";
  }

  private String getOutOfDateReleaseUpdates() {
    return "<html><body>" + "Link to full Change Log:<br /><a class=\"external\" href=\"" + changelogLink + "\">"
        + changelogLink + "</a><br />"
        + "</body></html>";
  }

  Component getOutOfDateComponent() {
    final JPanel panel = new JPanel(new BorderLayout());
    final JEditorPane intro = new JEditorPane("text/html", getOutOfDateMessage());
    intro.setEditable(false);
    intro.setOpaque(false);
    intro.setBorder(BorderFactory.createEmptyBorder());
    final HyperlinkListener hyperlinkListener = e -> {
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
    // scroll.setBorder(BorderFactory.createEmptyBorder());
    panel.add(scroll, BorderLayout.CENTER);
    final Dimension maxDimension = panel.getPreferredSize();
    maxDimension.width = Math.min(maxDimension.width, 700);
    maxDimension.height = Math.min(maxDimension.height, 480);
    panel.setMaximumSize(maxDimension);
    panel.setPreferredSize(maxDimension);
    return panel;
  }
}
