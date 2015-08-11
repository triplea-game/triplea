package games.puzzle.slidingtiles.ui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;

/**
 * Represents the menu bar for an n-puzzle game.
 */
public class NPuzzleMenu extends GridGameMenu<GridGameFrame> {
  private static final long serialVersionUID = -5247149106915478051L;

  public NPuzzleMenu(final GridGameFrame frame) {
    super(frame);
  }

  /**
   * @param parentMenu
   */
  @Override
  protected void addHowToPlayHelpMenu(final JMenu parentMenu) {
    parentMenu.add(new AbstractAction("How to play...") {
      private static final long serialVersionUID = -7535795861423393750L;

      @Override
      public void actionPerformed(final ActionEvent e) {
        // html formatted string
        final String hints = "<p><b>Winning</b><br>"
            + "Rearrange the tiles into numerical order, with the blank square in the upper left corner.</p>"
            + "<p><b>Moving:</b><br>"
            + "Any square which is horizontally or vertically adjacent to the blank square may be moved into the blank square</p>";
        final JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(hints);
        editorPane.setPreferredSize(new Dimension(550, 380));
        final JScrollPane scroll = new JScrollPane(editorPane);
        JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
      }
    });
  }
}
