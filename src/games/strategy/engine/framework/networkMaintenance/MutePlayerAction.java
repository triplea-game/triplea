package games.strategy.engine.framework.networkMaintenance;

import games.strategy.engine.framework.startup.login.ClientLoginValidator;
import games.strategy.net.*;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.TreeSet;

import javax.swing.*;

public class MutePlayerAction extends AbstractAction
{
    private final Component m_parent;

    private final IServerMessenger m_messenger;

    public MutePlayerAction(Component parent, IServerMessenger messenger)
    {
        super("Mute Player's Chatting...");
        m_parent = JOptionPane.getFrameForComponent(parent);
        m_messenger = messenger;
    }

    public void actionPerformed(ActionEvent e)
    {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        JComboBox combo = new JComboBox(model);
        model.addElement("");

        for (INode node : new TreeSet<INode>(m_messenger.getNodes()))
        {
            if (!node.equals(m_messenger.getLocalNode()))
                model.addElement(node.getName());
        }

        if (model.getSize() == 1)
        {
            JOptionPane.showMessageDialog(m_parent, "No remote players", "No Remote Players", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int rVal = JOptionPane.showConfirmDialog(m_parent, combo, "Select player to mute", JOptionPane.OK_CANCEL_OPTION);
        if (rVal != JOptionPane.OK_OPTION)
            return;

        String name = (String) combo.getSelectedItem();

        for (INode node : m_messenger.getNodes())
        {
            if (node.getName().equals(name))
            {
                ServerMessenger.getInstance().NotifyIPMutingOfPlayer(node.getAddress().getHostAddress(), Long.MAX_VALUE);
                ServerMessenger.getInstance().NotifyMacMutingOfPlayer(ServerMessenger.getInstance().GetMacAddressesOfPlayers().get(node.getName()), Long.MAX_VALUE);
                return;
            }
        }

    }
}