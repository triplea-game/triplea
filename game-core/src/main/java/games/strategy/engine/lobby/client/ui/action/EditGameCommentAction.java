package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;

/**
 * A UI action that prompts the user to change the comment displayed in the lobby game list for the
 * selected game host.
 */
public class EditGameCommentAction extends AbstractAction {
  private static final long serialVersionUID = 1723950003185387843L;
  private final InGameLobbyWatcherWrapper lobbyWatcher;
  private final Component parent;

  public EditGameCommentAction(final InGameLobbyWatcherWrapper watcher, final Component parent) {
    super("Set Lobby Comment...");
    this.parent = parent;
    lobbyWatcher = watcher;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (!lobbyWatcher.isActive()) {
      setEnabled(false);
      JOptionPane.showMessageDialog(
          JOptionPane.getFrameForComponent(parent), "Not connected to Lobby");
      return;
    }
    final String current = lobbyWatcher.getComments();
    final String gameComments =
        JOptionPane.showInputDialog(
            JOptionPane.getFrameForComponent(parent), "Edit the comments for the game", current);
    if (gameComments != null) {
      lobbyWatcher.setGameComments(gameComments);
    }
  }
}
