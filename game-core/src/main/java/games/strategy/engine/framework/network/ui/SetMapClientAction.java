package games.strategy.engine.framework.network.ui;

import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;

/** An action for changing the map across all network nodes from a client node. */
public class SetMapClientAction extends AbstractAction {
  private static final long serialVersionUID = -9156920997678163614L;

  final List<String> availableGames;
  private final Component parent;
  private final IServerStartupRemote serverStartupRemote;

  public SetMapClientAction(
      final Component parent,
      final IServerStartupRemote serverStartupRemote,
      final List<String> availableGames) {
    super("Change Game To");
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.serverStartupRemote = serverStartupRemote;
    this.availableGames = availableGames;
    Collections.sort(this.availableGames);
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    final JComboBox<String> combo = new JComboBox<>(model);
    model.addElement("");
    for (final String game : availableGames) {
      model.addElement(game);
    }
    if (model.getSize() <= 1) {
      JOptionPane.showMessageDialog(
          parent, "No available games", "No available games", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final int selectedOption =
        JOptionPane.showConfirmDialog(
            parent, combo, "Change Game To: ", JOptionPane.OK_CANCEL_OPTION);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = (String) combo.getSelectedItem();
    if (name == null || name.length() <= 1) {
      return;
    }
    serverStartupRemote.changeServerGameTo(name);
  }
}
