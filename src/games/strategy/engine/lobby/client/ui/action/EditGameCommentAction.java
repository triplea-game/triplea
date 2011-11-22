package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

public class EditGameCommentAction extends AbstractAction
{
	private final InGameLobbyWatcher m_lobbyWatcher;
	private final Component m_parent;
	
	public EditGameCommentAction(final InGameLobbyWatcher watcher, final Component parent)
	{
		super("Set Lobby Comment...");
		m_parent = parent;
		m_lobbyWatcher = watcher;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		if (!m_lobbyWatcher.isActive())
		{
			setEnabled(false);
			JOptionPane.showMessageDialog(JOptionPane.getFrameForComponent(m_parent), "Not connected to Lobby");
			return;
		}
		final String current = m_lobbyWatcher.getComments();
		final String rVal = JOptionPane.showInputDialog(JOptionPane.getFrameForComponent(m_parent), "Edit the comments for the game", current);
		if (rVal != null)
		{
			m_lobbyWatcher.setGameComments(rVal);
		}
	}
}
