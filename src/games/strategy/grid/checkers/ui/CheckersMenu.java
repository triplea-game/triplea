package games.strategy.grid.checkers.ui;

import games.strategy.grid.ui.GridGameFrame;
import games.strategy.grid.ui.GridGameMenu;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

/**
 * 
 * @author veqryn
 * 
 */
public class CheckersMenu extends GridGameMenu<GridGameFrame>
{
	private static final long serialVersionUID = 2529708521174152340L;
	
	public CheckersMenu(final GridGameFrame frame)
	{
		super(frame);
	}
	
	/**
	 * @param parentMenu
	 */
	@Override
	protected void addHowToPlayHelpMenu(final JMenu parentMenu)
	{
		parentMenu.add(new AbstractAction("How to play...")
		{
			private static final long serialVersionUID = -561502556482560961L;
			
			public void actionPerformed(final ActionEvent e)
			{
				// html formatted string
				final String hints = "<p><b>Checkers</b> "
							+ "<br />http://en.wikipedia.org/wiki/Checkers</p> "
							+ "<br /><br /><b>How To Move Pieces</b> "
							+ "<br />Click once on the piece (do not hold the mouse button down). "
							+ "<br />Then Click once on where you want the piece to go. "
							+ "<br />To Jump Over other pieces, right click (or shift/ctrl click) on those tiles in the middle. "
							+ "<br /><br />So for example, we want to move from 0,0 to 2,2 to 4,4 (we are hopping over 2 pieces, at 1,1 and 3,3), "
							+ "<br />Then we would click once on 0,0 then right click (or shift/ctrl click) on 2,2  then normal click on 4,4. "
							+ "<br /><br /><br /><b>The Goal of Checkers</b> "
							+ "<br />Checkers is a game played between two opponents on opposite sides of a board containing 64 squares of alternating colors. "
							+ "Each player has 12 pieces. "
							+ "Victory is achieved when you have captured all of the opponent's pieces, or prevented them from making any legal moves. "
							+ "<br /><br /><b>How the Checkers Pieces Move</b> "
							+ "<br />There are two types of pieces: pawns (men), and kings.  All pieces start as pawns. "
							+ "<br />1. A simple move involves sliding a piece one space diagonally forward to an adjacent unoccupied dark square. "
							+ "<br />2. A jump is a move from a square diagonally adjacent to one of the opponent's pieces to an empty square immediately and directly on the opposite side of the opponent's square, "
							+ "thus jumping directly over the square containing the opponent's piece. "
							+ "<br />An uncrowned piece may only jump diagonally forward, kings may also jump diagonally backward. A piece that is jumped is captured and removed from the board. "
							+ "<br />Multiple-jump moves are possible if when the jumping piece lands, there is another immediate piece that can be jumped; even if the jump is in a different direction. "
							+ "<br />Jumping is mandatory: whenever a player has the option to jump, that person must jump (even if it's to the jumping player's disadvantage; "
							+ "for example, a player can choose to allow one of his men to get captured to set up capturing two or more of his/her opponent's men)."
							+ "<br />When multiple-option jumping moves are available, whether with the one piece in different directions or multiple pieces that can make various jumping moves, "
							+ "the player may choose which piece to jump with and which jumping option or sequence of jumps to make. "
							+ "The jumping sequence chosen does not necessarily have to be the one that would have resulted in the most captures; "
							+ "however, one must make all available captures in the chosen sequence. Any piece, whether it is a king or not, can jump a king. "
							+ "<br /><br /><b>Kings</b>"
							+ "<br />If a player's piece moves into the kings row (last row) on the opposing player's side of the board, that piece is said to be crowned, "
							+ "becoming a king and gaining the ability to move both forward and backward. "
							+ "If a player's piece jumps into the kings row, the current move terminates; having just been crowned, the piece cannot continue on by jumping back out (as in a multiple jump), until the next move. ";
				final JEditorPane editorPane = new JEditorPane();
				editorPane.setEditable(false);
				editorPane.setContentType("text/html");
				editorPane.setText(hints);
				editorPane.setPreferredSize(new Dimension(550, 380));
				editorPane.setCaretPosition(0);
				final JScrollPane scroll = new JScrollPane(editorPane);
				JOptionPane.showMessageDialog(m_frame, scroll, "Movement Help", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}
}
