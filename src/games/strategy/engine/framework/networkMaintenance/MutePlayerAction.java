package games.strategy.engine.framework.networkMaintenance;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import games.strategy.net.ServerMessenger;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

public class MutePlayerAction extends AbstractAction
{
	private final Component m_parent;
	private final IServerMessenger m_messenger;
	
	public MutePlayerAction(final Component parent, final IServerMessenger messenger)
	{
		super("Mute Player's Chatting...");
		m_parent = JOptionPane.getFrameForComponent(parent);
		m_messenger = messenger;
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final DefaultComboBoxModel model = new DefaultComboBoxModel();
		final JComboBox combo = new JComboBox(model);
		model.addElement("");
		for (final INode node : new TreeSet<INode>(m_messenger.getNodes()))
		{
			if (!node.equals(m_messenger.getLocalNode()))
				model.addElement(node.getName());
		}
		if (model.getSize() == 1)
		{
			JOptionPane.showMessageDialog(m_parent, "No remote players", "No Remote Players", JOptionPane.ERROR_MESSAGE);
			return;
		}
		final int rVal = JOptionPane.showConfirmDialog(m_parent, combo, "Select player to mute", JOptionPane.OK_CANCEL_OPTION);
		if (rVal != JOptionPane.OK_OPTION)
			return;
		final String name = (String) combo.getSelectedItem();
		for (final INode node : m_messenger.getNodes())
		{
			if (node.getName().equals(name))
			{
				final String realName = node.getName().split(" ")[0];
				ServerMessenger.getInstance().NotifyUsernameMutingOfPlayer(realName, null);
				ServerMessenger.getInstance().NotifyIPMutingOfPlayer(node.getAddress().getHostAddress(), null);
				ServerMessenger.getInstance().NotifyMacMutingOfPlayer(ServerMessenger.getInstance().GetPlayerMac(node.getName()), null);
				return;
			}
		}
	}
}
