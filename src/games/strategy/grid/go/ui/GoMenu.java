package games.strategy.grid.go.ui;

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
public class GoMenu extends GridGameMenu<GridGameFrame>
{
	private static final long serialVersionUID = 2522152740134093334L;
	
	public GoMenu(final GridGameFrame frame)
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
			private static final long serialVersionUID = 4760939530305280882L;
			
			public void actionPerformed(final ActionEvent e)
			{
				// html formatted string
				final String hints = "<p><b>Go</b> "
							+ "<br />http://en.wikipedia.org/wiki/Go</p> "
							+ "<br /><br /><b>How To Place Pieces</b> "
							+ "<br />Click on the intersection where you want it to go. "
							+ "<br /><br /><br /><b>The Goal of Go</b> "
							+ "<br />";
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
