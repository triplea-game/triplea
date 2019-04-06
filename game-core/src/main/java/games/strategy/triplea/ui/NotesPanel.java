package games.strategy.triplea.ui;

import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import games.strategy.triplea.ui.menubar.HelpMenu;

class NotesPanel extends JPanel {
  private static final long serialVersionUID = 2746643868463714526L;
  private final JEditorPane gameNotesPane;

  // we now require passing a JEditorPane containing the notes in it, because we do not want to have multiple copies of
  // it in memory for all the different ways the user can access the game notes
  // so instead we keep the main copy in the TripleAMenuBar, and then give it to the notes tab. this prevents out of
  // memory errors for maps with large images in their games notes.
  NotesPanel(final String trimmedNotes) {
    gameNotesPane = HelpMenu.createNotesPanel(trimmedNotes);
    initLayout();
  }

  protected void initLayout() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    final JScrollPane scroll = new JScrollPane(gameNotesPane);
    scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    add(scroll);
  }
}
