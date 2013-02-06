/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package games.puzzle.tictactoe.ui;

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
 * @author Lane Schwartz
 * @version $LastChangedDate$
 */
public class TicTacToeMenu extends GridGameMenu<GridGameFrame>
{
	private static final long serialVersionUID = 1378128476597319634L;
	
	public TicTacToeMenu(final GridGameFrame frame)
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
			private static final long serialVersionUID = 3734484281045120725L;
			
			public void actionPerformed(final ActionEvent e)
			{
				// html formatted string
				final String hints = "<p><b>Winning</b><br>" + "White wins by moving the king into a corner square.<br>" + "Black wins by capturing the king.</p>" + "<p><b>Moving:</b><br>"
							+ "All pieces move like rooks in chess.<br>" + "Click on a piece, then drag it and drop it on an empty square.</p>" + "<p><b>Capturing the King</b><br>"
							+ "The king can be captured by surrounding it on all four sides with opposing pieces, "
							+ "or by surrounding it on three sides with opposing pieces, with the center square on the fourth side. " + "<p><b>Capturing a Pawn</b><br>"
							+ "A pawn can be captured by sandwiching it between two of the opposing side's pieces, " + "or by sandwiching it between an opposing piece and a corner square.<br>"
							+ "Note: If a pawn moves into one of these two situations, that does not cause it to be captured.<br>" + "Note: The king may participate in a capture.</p>"
							+ "<p><b>Restrictions</b><br>" + "Only the king may occupy the center square and the corner squares.<br>"
							+ "Pawns may not occupy the center square or the corner squares.<br>" + "However, any piece may move through the center square.</p>";
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
