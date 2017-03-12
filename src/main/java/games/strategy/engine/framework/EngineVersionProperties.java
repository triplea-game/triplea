package games.strategy.engine.framework;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.net.OpenFileUtility;
import games.strategy.util.Version;

public class EngineVersionProperties {
  private final Version latestVersionOut;
  private final String link;
  private final String linkAlt;
  private final String changelogLink;
  private static final String TRIPLEA_VERSION_LINK =
      "https://raw.githubusercontent.com/triplea-game/triplea/master/latest_version.properties";

  private EngineVersionProperties() {
    this(getProperties());
  }

  private EngineVersionProperties(final Properties props) {
    latestVersionOut =
        new Version(props.getProperty("LATEST", ClientContext.engineVersion().getVersion().toStringFull(".")));
    link = props.getProperty("LINK", "http://triplea-game.github.io/");
    linkAlt = props.getProperty("LINK_ALT", "http://triplea-game.github.io/download/");
    changelogLink = props.getProperty("CHANGELOG", "http://triplea-game.github.io/release_notes/");
  }

  public static EngineVersionProperties contactServerForEngineVersionProperties() {
    // sourceforge sometimes takes a long while to return results
    // so run a couple requests in parallel, starting with delays to try and get a response quickly
    final AtomicReference<EngineVersionProperties> ref = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    for (int i = 0; i < 5; i++) {
      new Thread(() -> {
        ref.set(new EngineVersionProperties());
        latch.countDown();
      }).start();
      try {
        latch.await(2, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
      }
      if (ref.get() != null) {
        break;
      }
    }
    // we have spawned a bunch of requests
    try {
      latch.await(15, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
    }
    return ref.get();
  }

  private static Properties getProperties() {
    final Properties props = new Properties();
    try {
      props.load(new URL(TRIPLEA_VERSION_LINK).openStream());
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed while attempting to check for a new Version", e);
    }
    return props;
  }

  public Version getLatestVersionOut() {
    return latestVersionOut;
  }

  public String getLinkToDownloadLatestVersion() {
    return link;
  }

  public String getLinkAltToDownloadLatestVersion() {
    return linkAlt;
  }

  public String getChangeLogLink() {
    return changelogLink;
  }

  private String getOutOfDateMessage() {
    final StringBuilder text = new StringBuilder("<html>");
    text.append("<h2>A new version of TripleA is out.  Please Update TripleA!</h2>");
    text.append("<br />Your current version: ").append(ClientContext.engineVersion().getFullVersion());
    text.append("<br />Latest version available for download: ").append(getLatestVersionOut());
    text.append("<br /><br />Click to download: <a class=\"external\" href=\"").append(getLinkToDownloadLatestVersion())
        .append("\">").append(getLinkToDownloadLatestVersion()).append("</a>");
    text.append("<br />Backup Mirror: <a class=\"external\" href=\"").append(getLinkAltToDownloadLatestVersion())
        .append("\">").append(getLinkAltToDownloadLatestVersion()).append("</a>");
    text.append(
        "<br /><br />Please note that installing a new version of TripleA will not remove any old copies of TripleA."
            + "<br />So be sure to either manually uninstall all older versions of TripleA, or change your shortcuts to the new TripleA.");
    text.append("<br /><br />What is new:<br />");
    text.append("</html>");
    return text.toString();
  }

  private String getOutOfDateReleaseUpdates() {
    final StringBuilder text = new StringBuilder("<html><body>");
    text.append("Link to full Change Log:<br /><a class=\"external\" href=\"").append(getChangeLogLink()).append("\">")
        .append(getChangeLogLink()).append("</a><br />");
    text.append("</body></html>");
    return text.toString();
  }

  public Component getOutOfDateComponent() {
    final JPanel panel = new JPanel(new BorderLayout());
    final JEditorPane intro = new JEditorPane("text/html", getOutOfDateMessage());
    intro.setEditable(false);
    intro.setOpaque(false);
    intro.setBorder(BorderFactory.createEmptyBorder());
    final HyperlinkListener hyperlinkListener = e -> {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
        OpenFileUtility.openURL(e.getDescription());
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
