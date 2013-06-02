package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.framework.startup.ui.GameSelectorPanel;
import games.strategy.net.IClientMessenger;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * 
 * @author veqryn
 * 
 */
public class ChangeGameToSaveGameClientAction extends AbstractAction
{
	private static final long serialVersionUID = -6986376382381381377L;
	private final Component m_parent;
	private final IClientMessenger m_clientMessenger;
	
	public ChangeGameToSaveGameClientAction(final Component parent, final IClientMessenger clientMessenger)
	{
		super("Change To Gamesave (Load Game)...");
		m_parent = JOptionPane.getFrameForComponent(parent);
		m_clientMessenger = clientMessenger;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final File file = GameSelectorPanel.selectGameFile(JOptionPane.getFrameForComponent(m_parent));
		if (file == null || !file.exists())
			return;
		m_clientMessenger.changeToGameSave(file, file.getName());
	}
}
