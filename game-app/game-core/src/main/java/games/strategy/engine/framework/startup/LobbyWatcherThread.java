package games.strategy.engine.framework.startup;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.net.IServerMessenger;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.http.client.web.socket.client.connections.GameToLobbyConnection;

@AllArgsConstructor
public class LobbyWatcherThread {
  @Getter private final InGameLobbyWatcherWrapper lobbyWatcher = new InGameLobbyWatcherWrapper();
  @Nonnull private final GameSelectorModel gameSelectorModel;
  @Nonnull private final IServerMessenger serverMessenger;
  @Nonnull private final WatcherThreadMessaging watcherThreadMessaging;

  public Optional<String> getGameId() {
    return lobbyWatcher.getGameId();
  }

  /** Creates, connects, the lobby thread to the lobby. */
  public void createLobbyWatcher(
      final GameToLobbyConnection gameToLobbyConnection, final boolean isHuman) {
    InGameLobbyWatcher.newInGameLobbyWatcher(
            serverMessenger,
            gameToLobbyConnection,
            watcherThreadMessaging,
            lobbyWatcher.getInGameLobbyWatcher(),
            isHuman)
        .ifPresent(
            watcher -> {
              watcher.setGameSelectorModel(gameSelectorModel);
              lobbyWatcher.setInGameLobbyWatcher(watcher);
            });
  }
}
