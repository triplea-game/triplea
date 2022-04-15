package games.strategy.engine.framework.ui;

import java.awt.Color;
import java.awt.Rectangle;
import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

/** Component for displaying HTML-formatted game notes. */
public class GameNotesView extends JEditorPane {
  public GameNotesView() {
    setEditable(false);
    setContentType("text/html");
    // If the foreground color isn't set, a dark theme may make the text color white, which will be
    // unreadable if the html sets a light background color.
    setForeground(Color.BLACK);
    // If the background color isn't set, a dark theme may have a very dark window background, which
    // will make it hard to read the black text against. Note: For some reason, some dark themes
    // show the background as light gray even if white is set, but it doesn't actually look too bad.
    setBackground(Color.WHITE);
  }

  @Override
  public void setText(String text) {
    super.setText(text);
    // Scroll to the top of the notes screen when the text is updated.
    SwingUtilities.invokeLater(() -> scrollRectToVisible(new Rectangle()));
  }
}
