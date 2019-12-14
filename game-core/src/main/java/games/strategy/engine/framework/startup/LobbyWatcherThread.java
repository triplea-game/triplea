package games.strategy.engine.framework.startup;

import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;

import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcher;
import games.strategy.engine.framework.startup.ui.InGameLobbyWatcherWrapper;
import games.strategy.engine.framework.startup.ui.LocalServerAvailabilityCheck;
import games.strategy.net.IServerMessenger;
import java.net.URI;
import javax.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;
import org.triplea.http.client.lobby.game.listing.GameListingClient;

@AllArgsConstructor
public class LobbyWatcherThread {
  @Getter private final InGameLobbyWatcherWrapper lobbyWatcher = new InGameLobbyWatcherWrapper();
  @Nonnull private final GameSelectorModel gameSelectorModel;
  @Nonnull private final IServerMessenger serverMessenger;
  @Nonnull private final WatcherThreadMessaging watcherThreadMessaging;

  public void createLobbyWatcher(
      final URI lobbyUri, final GameHostingResponse gameHostingResponse) {

    final HttpLobbyClient lobbyClient =
        HttpLobbyClient.newClient(lobbyUri, ApiKey.of(gameHostingResponse.getApiKey()));

    InGameLobbyWatcher.newInGameLobbyWatcher(
            serverMessenger,
            gameHostingResponse,
            GameListingClient.newClient(lobbyUri, ApiKey.of(gameHostingResponse.getApiKey())),
            watcherThreadMessaging::connectionLostReporter,
            watcherThreadMessaging::connectionReEstablishedReporter,
            lobbyWatcher.getInGameLobbyWatcher())
        .ifPresent(
            watcher -> {
              watcher.setGameSelectorModel(gameSelectorModel);
              lobbyWatcher.setInGameLobbyWatcher(watcher);

              LocalServerAvailabilityCheck.builder()
                  .connectivityCheckClient(lobbyClient.getConnectivityCheckClient())
                  .localPort(serverMessenger.getLocalNode().getPort())
                  .errorHandler(watcherThreadMessaging::serverNotAvailableHandler)
                  .build()
                  .run();

              System.clearProperty(TRIPLEA_NAME);
            });
  }
}
