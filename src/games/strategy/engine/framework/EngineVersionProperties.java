package games.strategy.engine.framework;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.net.DesktopUtilityBrowserLauncher;
import games.strategy.util.Version;

public class EngineVersionProperties {
  private final Version m_latestVersionOut;
  private final Version m_showUpdatesFrom;
  private final Map<Version, String> m_releaseNotes;
  private final String m_link;
  private final String m_linkAlt;
  private final String m_changelogLink;
  private volatile boolean m_done = false;
  private static final String s_linkToTripleA = "http://triplea.sourceforge.net/latest/latest_version.properties";

  private EngineVersionProperties(final URL url) {
    this(getProperties(url));
  }

  private EngineVersionProperties(final Properties props) {
    m_latestVersionOut = new Version(props.getProperty("LATEST", ClientContext.engineVersion().getVersion().toStringFull(".")));
    final Version showUpdatesFromTemp =
        new Version(props.getProperty("SHOW_FROM", ClientContext.engineVersion().getVersion().toStringFull(".")));
    m_showUpdatesFrom =
        (ClientContext.engineVersion().getVersion().isLessThan(showUpdatesFromTemp) ? ClientContext.engineVersion().getVersion() : showUpdatesFromTemp);
    m_link = props.getProperty("LINK", "http://triplea.sourceforge.net/");
    m_linkAlt = props.getProperty("LINK_ALT", "http://sourceforge.net/projects/tripleamaps/files/TripleA/stable/");
    m_changelogLink = props.getProperty("CHANGELOG",
        "https://triplea.svn.sourceforge.net/svnroot/triplea/trunk/triplea/changelog.txt");
    m_releaseNotes = new HashMap<>();
    for (final Entry<Object, Object> entry : props.entrySet()) {
      final String key = (String) entry.getKey();
      if (key != null && key.length() > 6 && key.startsWith("NOTES_")) {
        final Version version = new Version(key.substring(6));
        final String value = (String) entry.getValue();
        if (value != null && value.trim().length() > 0) {
          m_releaseNotes.put(version, value);
        }
      }
    }
    m_done = true;
  }

  public static EngineVersionProperties contactServerForEngineVersionProperties() {
    final URL engineversionPropsURL;
    try {
      engineversionPropsURL = new URL(s_linkToTripleA);
    } catch (final MalformedURLException e) {
      ClientLogger.logQuietly(e);
      return new EngineVersionProperties(new Properties());
    }
    return contactServerForEngineVersionProperties(engineversionPropsURL);
  }

  private static EngineVersionProperties contactServerForEngineVersionProperties(final URL engineversionPropsURL) {
    // sourceforge sometimes takes a long while to return results
    // so run a couple requests in parallel, starting with delays to try and get a response quickly
    final AtomicReference<EngineVersionProperties> ref = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        for (int i = 0; i < 5; i++) {
          spawnRequest(engineversionPropsURL, ref, latch);
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
      }

      private void spawnRequest(final URL engineversionPropsURL, final AtomicReference<EngineVersionProperties> ref,
          final CountDownLatch latch) {
        final Thread t1 = new Thread(() -> {
          ref.set(new EngineVersionProperties(engineversionPropsURL));
          latch.countDown();
        });
        t1.start();
      }
    };
    r.run();
    final EngineVersionProperties props = ref.get();
    return props;
  }

  private static Properties getProperties(final URL url) {
    Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.SEVERE);
    final Properties props = new Properties();
    final HttpClient client = new HttpClient();
    final HostConfiguration config = client.getHostConfiguration();
    config.setHost(url.getHost());
    // add the proxy
    GameRunner2.addProxy(config);
    final GetMethod method = new GetMethod(url.getPath());
    // pretend to be ie
    method.setRequestHeader("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
    try {
      client.executeMethod(method);
      final String propsString = method.getResponseBodyAsString();
      props.load(new ByteArrayInputStream(propsString.getBytes()));
    } catch (final Exception ioe) {
      // do nothing, we return an empty properties
    } finally {
      method.releaseConnection();
    }
    Logger.getLogger("org.apache.commons.httpclient").setLevel(Level.INFO);
    return props;
  }

  public boolean isDone() {
    return m_done;
  }

  public Version getLatestVersionOut() {
    return m_latestVersionOut;
  }

  public Version getShowUpdatesFrom() {
    return m_showUpdatesFrom;
  }

  public String getLinkToDownloadLatestVersion() {
    return m_link;
  }

  public String getLinkAltToDownloadLatestVersion() {
    return m_linkAlt;
  }

  public String getChangeLogLink() {
    return m_changelogLink;
  }

  public Map<Version, String> getReleaseNotes() {
    return m_releaseNotes;
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

  private String getOutOfDateReleaseUpdates(final boolean showAll) {
    final StringBuilder text = new StringBuilder("<html>");
    final List<Version> versions = new ArrayList<>();
    versions.addAll(getReleaseNotes().keySet());
    Collections.sort(versions, Version.getHighestToLowestComparator());
    for (final Version v : versions) {
      if (showAll || ClientContext.engineVersion().getVersion().isLessThan(v)) {
        text.append("<br />").append(getReleaseNotes().get(v)).append("<br /><br />");
      }
    }
    text.append("Link to full Change Log:<br /><a class=\"external\" href=\"").append(getChangeLogLink()).append("\">")
        .append(getChangeLogLink()).append("</a><br />");
    text.append("</html>");
    return text.toString();
  }

  public Component getCurrentFeaturesComponent() {
    final JPanel panel = new JPanel(new BorderLayout());
    final JEditorPane intro = new JEditorPane("text/html", "<html><h2>What is new in version " + ClientContext.engineVersion()
        + "</h2><br />" + "Please visit our forum to get involved: "
        + "<a class=\"external\" href=\"http://triplea.sourceforge.net/mywiki/Forum\">http://triplea.sourceforge.net/mywiki/Forum</a><br /><br /></html>");
    intro.setEditable(false);
    intro.setOpaque(false);
    intro.setBorder(BorderFactory.createEmptyBorder());
    final HyperlinkListener hyperlinkListener = e -> {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
        DesktopUtilityBrowserLauncher.openURL(e.getDescription());
      }
    };
    intro.addHyperlinkListener(hyperlinkListener);
    panel.add(intro, BorderLayout.NORTH);
    final StringBuilder releaseNotesBuilder = new StringBuilder("<html>");
    final List<Version> versions = new ArrayList<>();
    versions.addAll(getReleaseNotes().keySet());
    Collections.sort(versions, Version.getHighestToLowestComparator());
    for (final Version v : versions) {
      if (getShowUpdatesFrom().equals(v, false) || getShowUpdatesFrom().isLessThan(v)) {
        releaseNotesBuilder.append("<br />").append(getReleaseNotes().get(v)).append("<br /><br />");
      }
    }
    releaseNotesBuilder.append("Link to full Change Log:<br /><a class=\"external\" href=\"").append(getChangeLogLink())
        .append("\">").append(getChangeLogLink()).append("</a><br />");
    releaseNotesBuilder.append("</html>");
    final JEditorPane updates = new JEditorPane("text/html", releaseNotesBuilder.toString());
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

  public Component getOutOfDateComponent(final boolean showAll) {
    final JPanel panel = new JPanel(new BorderLayout());
    final JEditorPane intro = new JEditorPane("text/html", getOutOfDateMessage());
    intro.setEditable(false);
    intro.setOpaque(false);
    intro.setBorder(BorderFactory.createEmptyBorder());
    final HyperlinkListener hyperlinkListener = e -> {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
        DesktopUtilityBrowserLauncher.openURL(e.getDescription());
      }
    };
    intro.addHyperlinkListener(hyperlinkListener);
    panel.add(intro, BorderLayout.NORTH);
    final JEditorPane updates = new JEditorPane("text/html", getOutOfDateReleaseUpdates(showAll));
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
