package games.strategy.engine.framework.startup;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;

import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.LocalServerAvailabilityCheck;
import games.strategy.engine.framework.startup.ui.panels.main.game.selector.GameSelectorModel;
import games.strategy.engine.lobby.connection.GameToLobbyConnection;
import games.strategy.net.IServerMessenger;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class LobbyWatcherThread {
  @Getter private final InGameLobbyWatcherWrapper lobbyWatcher = new InGameLobbyWatcherWrapper();
  @Nonnull private final GameSelectorModel gameSelectorModel;
  @Nonnull private final IServerMessenger serverMessenger;
  @Nonnull private final WatcherThreadMessaging watcherThreadMessaging;

  public void createLobbyWatcher(final GameToLobbyConnection gameToLobbyConnection) {

    InGameLobbyWatcher.newInGameLobbyWatcher(
            serverMessenger,
            gameToLobbyConnection,
            watcherThreadMessaging::connectionLostReporter,
            watcherThreadMessaging::connectionReEstablishedReporter,
            lobbyWatcher.getInGameLobbyWatcher())
        .ifPresent(
            watcher -> {
              watcher.setGameSelectorModel(gameSelectorModel);
              lobbyWatcher.setInGameLobbyWatcher(watcher);

              LocalServerAvailabilityCheck.builder()
                  .gameToLobbyConnection(gameToLobbyConnection)
                  .localPort(serverMessenger.getLocalNode().getPort())
                  .errorHandler(watcherThreadMessaging::serverNotAvailableHandler)
                  .build()
                  .run();

              System.clearProperty(TRIPLEA_NAME);
            });
  }
}
