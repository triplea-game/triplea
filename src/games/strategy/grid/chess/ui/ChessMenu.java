package games.strategy.grid.chess.ui;

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
public class ChessMenu extends GridGameMenu<GridGameFrame>
{
	private static final long serialVersionUID = 348421633415234065L;
	
	public ChessMenu(final GridGameFrame frame)
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
				final String hints = "<p><b>Chess</b><br />"
							+ "http://en.wikipedia.org/wiki/Rules_of_chess.</p>";
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
