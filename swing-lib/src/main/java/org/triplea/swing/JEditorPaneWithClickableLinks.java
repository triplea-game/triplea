package org.triplea.swing;

import javax.swing.JEditorPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import org.triplea.awt.OpenFileUtility;

/**
 * Creates a JEditorPane with clickable HTML links. Text content accepted is HTML, the links should
 * be HTML style anchor tags with an href.
 */
public class JEditorPaneWithClickableLinks extends JEditorPane {

  public JEditorPaneWithClickableLinks() {
    this("");
  }

  public JEditorPaneWithClickableLinks(final String htmlTextContent) {
    super("text/html", htmlTextContent);
    setEditable(false);
    setOpaque(false);
    setBorder(new EmptyBorder(10, 0, 20, 0));
    addHyperlinkListener(
        e -> {
          if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            OpenFileUtility.openUrl(e.getURL().toString());
          }
        });
  }

  /** Creates an HTML anchor tag (a link) with given title and link location. */
  public static String toLink(final String title, final String link) {
    return String.format("<a href=\"%s\">%s</a>", link, title);
  }
}
