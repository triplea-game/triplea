package games.strategy.chess.ui;

import games.strategy.common.ui.BasicGameMenuBar;
import games.strategy.engine.data.properties.PropertiesUI;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

public class ChessMenu extends BasicGameMenuBar<ChessFrame>
{
	private static final long serialVersionUID = 348421633415234065L;
	
	public ChessMenu(final ChessFrame frame)
	{
		super(frame);
	}
	
	@Override
	protected void createGameSpecificMenus(final JMenuBar menuBar)
	{
		createGameMenu(menuBar);
	}
	
	@Override
	protected void addGameSpecificHelpMenus(final JMenu helpMenu)
	{
		addHowToPlayHelpMenu(helpMenu);
	}
	
	/**
	 * @param menuGame
	 */
	private void createGameMenu(final JMenuBar menuBar)
	{
		if (!getGame().getData().getProperties().getEditableProperties().isEmpty())
		{
			final JMenu menuGame = new JMenu("Game");
			menuBar.add(menuGame);
			final AbstractAction optionsAction = new AbstractAction("View Game Options...")
			{
				private static final long serialVersionUID = -6969050839395256643L;
				
				public void actionPerformed(final ActionEvent e)
				{
					final PropertiesUI ui = new PropertiesUI(getGame().getData().getProperties(), false);
					JOptionPane.showMessageDialog(m_frame, ui, "Game options", JOptionPane.PLAIN_MESSAGE);
				}
			};
			menuGame.add(optionsAction);
		}
	}
	
	/**
	 * @param parentMenu
	 */
	private void addHowToPlayHelpMenu(final JMenu parentMenu)
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
