package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

/**
 * A UI action that removes the selected game host from the lobby. There is no prompt to confirm the
 * action.
 */
public class RemoveGameFromLobbyAction extends AbstractAction {
  private static final long serialVersionUID = 8802420945692279375L;
  private final InGameLobbyWatcherWrapper lobbyWatcher;

  public RemoveGameFromLobbyAction(final InGameLobbyWatcherWrapper watcher) {
    super("Remove Game From Lobby");
    lobbyWatcher = watcher;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    lobbyWatcher.shutDown();
    setEnabled(false);
  }
}
