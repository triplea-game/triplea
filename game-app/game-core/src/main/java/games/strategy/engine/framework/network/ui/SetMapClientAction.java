package games.strategy.engine.framework.network.ui;

import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import java.awt.Component;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import org.triplea.java.ThreadRunner;

/** A class for changing the map across all network nodes from a client node. */
public class SetMapClientAction {

  final List<String> availableGames;
  private final Component parent;
  private final IServerStartupRemote serverStartupRemote;

  public SetMapClientAction(
      final Component parent,
      final IServerStartupRemote serverStartupRemote,
      final Collection<String> games) {
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.serverStartupRemote = serverStartupRemote;
    this.availableGames = games.stream().sorted().collect(Collectors.toList());
  }

  public void run() {
    if (availableGames.isEmpty()) {
      JOptionPane.showMessageDialog(
          parent, "No available games", "No available games", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    final JComboBox<String> combo = new JComboBox<>(model);
    model.addElement("");
    model.addAll(availableGames);
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
    // don't block UI thread
    ThreadRunner.runInNewThread(() -> serverStartupRemote.changeServerGameTo(name));
  }
}
