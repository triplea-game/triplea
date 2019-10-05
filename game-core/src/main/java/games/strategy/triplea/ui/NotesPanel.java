package games.strategy.triplea.ui;

import java.awt.Color;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import org.triplea.util.LocalizeHtml;

/** Wrapper class to display game related notes in a swing component. */
public class NotesPanel extends JScrollPane {
  private static final long serialVersionUID = 2746643868463714526L;

  public NotesPanel(final String trimmedNotes) {
    super(createNotesPane(LocalizeHtml.localizeImgLinksInHtml(trimmedNotes)));
  }

  private static JEditorPane createNotesPane(final String html) {
    final JEditorPane gameNotesPane = new JEditorPane("text/html", html);
    gameNotesPane.setEditable(false);
    gameNotesPane.setForeground(Color.BLACK);
    gameNotesPane.setCaretPosition(0);
    return gameNotesPane;
  }
}
