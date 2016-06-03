package games.strategy.triplea.ui;

import java.awt.Component;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import games.strategy.common.swing.SwingAction;
import games.strategy.engine.data.GameData;

public class NotesPanel extends JPanel {
  private static final long serialVersionUID = 2746643868463714526L;
  protected final JEditorPane m_gameNotesPane;
  protected final GameData m_data;
  final JButton m_refresh = new JButton("Refresh Notes");

  // we now require passing a JEditorPane containing the notes in it, because we do not want to have multiple copies of
  // it in memory for all
  // the different ways the user can access the game notes
  // so instead we keep the main copy in the BasicGameMenuBar, and then give it to the notes tab. this prevents out of
  // memory errors for
  // maps with large images in their games notes.
  public NotesPanel(final GameData data, final JEditorPane gameNotesPane) {
    m_data = data;
    m_gameNotesPane = gameNotesPane;
    initLayout();
  }

  protected void initLayout() {
    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    m_refresh.setAlignmentY(Component.CENTER_ALIGNMENT);
    m_refresh.addActionListener(SwingAction.of("Refresh Notes", e -> {
      SwingUtilities.invokeLater(() -> layoutNotes());
    }));
    // layoutNotes();
    removeNotes();
  }

  void removeNotes() {
    NotesPanel.this.removeAll();
    NotesPanel.this.add(new JLabel(" "));
    NotesPanel.this.add(m_refresh);
    NotesPanel.this.add(new JLabel(" "));
    // NotesPanel.this.invalidate();
  }

  void layoutNotes() {
    if (m_gameNotesPane == null) {
      return;
    }
    NotesPanel.this.removeAll();
    NotesPanel.this.add(new JLabel(" "));
    NotesPanel.this.add(m_refresh);
    NotesPanel.this.add(new JLabel(" "));
    final JScrollPane scroll = new JScrollPane(m_gameNotesPane);
    scroll.scrollRectToVisible(new Rectangle(0, 0, 0, 0));
    NotesPanel.this.add(scroll);
    // NotesPanel.this.invalidate();
  }

  public boolean isEmpty() {
    return m_gameNotesPane == null || m_gameNotesPane.getText() == null || m_gameNotesPane.getText().length() <= 0;
  }
}
