package games.strategy.engine.framework.network.ui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;

/**
 * An action for muting a player in a network game.
 */
public class MutePlayerAction extends AbstractAction {
  private static final long serialVersionUID = -6578758359870435844L;
  private final Component parent;
  private final IServerMessenger messenger;

  public MutePlayerAction(final Component parent, final IServerMessenger messenger) {
    super("Mute Player's Chatting");
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.messenger = messenger;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    final JComboBox<String> combo = new JComboBox<>(model);
    model.addElement("");
    for (final INode node : new TreeSet<>(messenger.getNodes())) {
      if (!node.equals(messenger.getLocalNode())) {
        model.addElement(node.getName());
      }
    }
    if (model.getSize() == 1) {
      JOptionPane.showMessageDialog(parent, "No remote players", "No Remote Players", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final int selectedOption =
        JOptionPane.showConfirmDialog(parent, combo, "Select player to mute", JOptionPane.OK_CANCEL_OPTION);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = (String) combo.getSelectedItem();
    for (final INode node : messenger.getNodes()) {
      if (node.getName().equals(name)) {
        final String realName = node.getName().split(" ")[0];
        final String ip = node.getAddress().getHostAddress();
        final String mac = messenger.getPlayerMac(node.getName());
        messenger.notifyUsernameMutingOfPlayer(realName, null);
        messenger.notifyIpMutingOfPlayer(ip, null);
        messenger.notifyMacMutingOfPlayer(mac, null);
        return;
      }
    }
  }
}
