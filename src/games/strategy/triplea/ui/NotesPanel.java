package games.strategy.triplea.ui;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

public class NotesPanel extends VBox {
  protected final WebView m_gameNotesPane;

  // we now require passing a JEditorPane containing the notes in it, because we do not want to have multiple copies of
  // it in memory for all
  // the different ways the user can access the game notes
  // so instead we keep the main copy in the TripleAMenuBar, and then give it to the notes tab. this prevents out of
  // memory errors for
  // maps with large images in their games notes.
  public NotesPanel(final WebView gameNotesPane) {
    m_gameNotesPane = gameNotesPane;
    initLayout();
  }

  protected void initLayout() {
    removeNotes();
  }

  void removeNotes() {
    getChildren().clear();
  }

  void layoutNotes() {
    if (m_gameNotesPane == null) {
      return;
    }
    removeNotes();
    final ScrollPane scroll = new ScrollPane(m_gameNotesPane);
    getChildren().add(scroll);
  }

  public boolean isEmpty() {
    return m_gameNotesPane == null || m_gameNotesPane.getEngine().getDocument().getTextContent() == null
        || m_gameNotesPane.getEngine().getDocument().getTextContent().length() <= 0;
  }
}
