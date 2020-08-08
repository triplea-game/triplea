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

/** An action for booting a player from a network game. */
public class BootPlayerAction extends AbstractAction {
  private static final long serialVersionUID = 2799566047887167058L;
  private final Component parent;
  private final IServerMessenger messenger;

  public BootPlayerAction(final Component parent, final IServerMessenger messenger) {
    super("Remove Player");
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
            parent, combo, "Select player to remove", JOptionPane.OK_CANCEL_OPTION);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = (String) combo.getSelectedItem();
    for (final INode node : messenger.getNodes()) {
      if (node.getName().equals(name)) {
        messenger.removeConnection(node);
      }
    }
  }
}
