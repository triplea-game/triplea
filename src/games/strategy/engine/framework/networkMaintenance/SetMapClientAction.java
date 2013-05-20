package games.strategy.engine.framework.networkMaintenance;

import games.strategy.net.IClientMessenger;
import games.strategy.net.INode;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/**
 * 
 * @author veqryn
 * 
 */
public class SetMapClientAction extends AbstractAction
{
	private static final long serialVersionUID = -9156920997678163614L;
	private final Component m_parent;
	private final IClientMessenger m_clientMessenger;
	final List<String> m_availableGames;
	
	public SetMapClientAction(final Component parent, final IClientMessenger clientMessenger, final List<String> availableGames)
	{
		super("Change Game To...");
		m_parent = JOptionPane.getFrameForComponent(parent);
		m_clientMessenger = clientMessenger;
		m_availableGames = availableGames;
		Collections.sort(m_availableGames);
	}
	
	public void actionPerformed(final ActionEvent e)
	{
		final INode serverNode = m_clientMessenger.getServerNode();
		final DefaultComboBoxModel model = new DefaultComboBoxModel();
		final JComboBox combo = new JComboBox(model);
		model.addElement("");
		for (final String game : m_availableGames)
		{
			model.addElement(game);
		}
		if (serverNode == null || model.getSize() <= 1)
		{
			JOptionPane.showMessageDialog(m_parent, "No available games", "No available games", JOptionPane.ERROR_MESSAGE);
			return;
		}
		final int rVal = JOptionPane.showConfirmDialog(m_parent, combo, "Change Game To: ", JOptionPane.OK_CANCEL_OPTION);
		if (rVal != JOptionPane.OK_OPTION)
			return;
		final String name = (String) combo.getSelectedItem();
		if (name == null || name.length() <= 1)
			return;
		m_clientMessenger.changeServerGameTo(name);
	}
}
