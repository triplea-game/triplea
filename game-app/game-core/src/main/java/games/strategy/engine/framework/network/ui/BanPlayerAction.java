package games.strategy.engine.framework.network.ui;

import games.strategy.net.INode;
import games.strategy.net.IServerMessenger;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/** An action for banning a player from a network game. */
public class BanPlayerAction extends AbstractAction {
  private static final long serialVersionUID = -2415917785233191860L;
  private final Component parent;
  private final IServerMessenger messenger;

  public BanPlayerAction(final Component parent, final IServerMessenger messenger) {
    super("Ban Player From Game");
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
      JOptionPane.showMessageDialog(
          parent, "No remote players", "No Remote Players", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final int selectedOption =
        JOptionPane.showConfirmDialog(
            parent, combo, "Select player to ban", JOptionPane.OK_CANCEL_OPTION);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = (String) combo.getSelectedItem();
    for (final INode node : messenger.getNodes()) {
      if (node.getName().equals(name)) {
        final String ip = node.getAddress().getHostAddress();
        final String mac = messenger.getPlayerMac(node.getPlayerName());
        messenger.banPlayer(ip, mac);
        messenger.removeConnection(node);
        return;
      }
    }
  }
}
