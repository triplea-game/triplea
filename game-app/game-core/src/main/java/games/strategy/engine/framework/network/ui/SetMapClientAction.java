package games.strategy.engine.framework.network.ui;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.startup.mc.IServerStartupRemote;
import games.strategy.engine.framework.startup.mc.ServerModel;
import games.strategy.net.Messengers;
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
  private final Messengers messengers;

  public SetMapClientAction(final Component parent, final Messengers messengers) {
    this.parent = JOptionPane.getFrameForComponent(parent);
    this.messengers = messengers;
    this.availableGames =
        messengers
            .invokeRemoteMessage(
                ServerModel.SERVER_REMOTE_NAME,
                new IServerStartupRemote.GetAvailableGamesRequest(),
                IServerStartupRemote.GetAvailableGamesResponse.TYPE)
            .getAvailableGames()
            .stream()
            .sorted()
            .collect(Collectors.toList());
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
    ThreadRunner.runInNewThread(
        () ->
            messengers.invokeRemoteMessage(
                ServerModel.SERVER_REMOTE_NAME,
                new IServerStartupRemote.ChangeServerGameToRequest(name),
                IServerStartupRemote.ChangeServerGameToResponse.TYPE));
  }
}
