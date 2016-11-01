package games.strategy.engine.lobby.client.ui.action;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import javafx.scene.control.MenuItem;

public class RemoveGameFromLobbyAction extends MenuItem {
  private final InGameLobbyWatcherWrapper m_lobbyWatcher;

  public RemoveGameFromLobbyAction(final InGameLobbyWatcherWrapper watcher) {
    super("Remove Game From Lobby");
    m_lobbyWatcher = watcher;
    setOnAction(e -> {
      m_lobbyWatcher.shutDown();
      setDisable(false);
    });
  }
}
