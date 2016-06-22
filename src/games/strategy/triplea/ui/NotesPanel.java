package games.strategy.triplea.ui;

import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class NotesPanel extends JPanel {
  private static final long serialVersionUID = 2746643868463714526L;
  protected final JEditorPane m_gameNotesPane;

  // we now require passing a JEditorPane containing the notes in it, because we do not want to have multiple copies of
  // it in memory for all
  // the different ways the user can access the game notes
  // so instead we keep the main copy in the TripleAMenuBar, and then give it to the notes tab. this prevents out of
  // memory errors for
  // maps with large images in their games notes.
  public NotesPanel(final JEditorPane gameNotesPane) {
    m_gameNotesPane = gameNotesPane;
    initLayout();
  }

  protected void initLayout() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    removeNotes();
  }

  void removeNotes() {
    removeAll();
  }

  void layoutNotes() {
    if (m_gameNotesPane == null) {
      return;
    }
    removeAll();
    final JScrollPane scroll = new JScrollPane(m_gameNotesPane);
    scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    add(scroll);
  }

  public boolean isEmpty() {
    return m_gameNotesPane == null || m_gameNotesPane.getText() == null || m_gameNotesPane.getText().length() <= 0;
  }
}
