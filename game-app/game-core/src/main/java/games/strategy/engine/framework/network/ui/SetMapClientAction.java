package games.strategy.engine.framework.network.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import java.awt.Component;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import org.triplea.java.ThreadRunner;

/** A class for changing the map across all network nodes from a client node. */
public class SetMapClientAction {

  final List<String> availableGames;
  private final Component parent;
  private final IServerStartupRemote serverStartupRemote;

  public SetMapClientAction(
      final Component parent, final IServerStartupRemote serverStartupRemote) {
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.serverStartupRemote = serverStartupRemote;
    this.availableGames =
        serverStartupRemote.getAvailableGames().stream().sorted().collect(Collectors.toList());
  }

  public void run() {
    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Should be run on EDT!");
    if (availableGames.isEmpty()) {
      JOptionPane.showMessageDialog(
          parent, "No available games", "No available games", JOptionPane.ERROR_MESSAGE);
      return;
    }
    final DefaultListModel<String> model = new DefaultListModel<>();
    model.addAll(availableGames);
    final JList<String> gameList = new JList<>(model);
    gameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    gameList.setVisibleRowCount(15);
    final int selectedOption =
        JOptionPane.showConfirmDialog(
            parent, new JScrollPane(gameList), "Change Game To: ", JOptionPane.OK_CANCEL_OPTION);
    if (selectedOption != JOptionPane.OK_OPTION) {
      return;
    }
    final String name = gameList.getSelectedValue();
    if (name == null) {
      return;
    }
    // don't block UI thread
    ThreadRunner.runInNewThread(() -> serverStartupRemote.changeServerGameTo(name));
  }
}
