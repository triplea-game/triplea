package games.strategy.engine.lobby.connection;

import java.net.URI;
import java.util.function.Consumer;
import lombok.Getter;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.lobby.HttpLobbyClient;
import org.triplea.http.client.lobby.game.hosting.GameHostingResponse;
import org.triplea.http.client.lobby.game.listing.LobbyWatcherClient;

/**
 * Represents a connection from a hosted game to lobby. A hosted game can perform actions like send
 * a game update so that the lobby updates the game listing. A hosted game will receive messages
 * from lobby for example like a player banned notification.
 */
public class GameToLobbyConnection {

  private final HttpLobbyClient lobbyClient;
  private final LobbyWatcherClient lobbyWatcherClient;

  @Getter private final GameHostingResponse gameHostingResponse;

  public GameToLobbyConnection(
      final URI lobbyUri,
      final GameHostingResponse gameHostingResponse,
      final Consumer<String> errorHandler) {
    lobbyClient =
        HttpLobbyClient.newClient(
            lobbyUri, ApiKey.of(gameHostingResponse.getApiKey()), errorHandler);

    this.gameHostingResponse = gameHostingResponse;

    lobbyWatcherClient =
        LobbyWatcherClient.newClient(lobbyClient.getLobbyUri(), lobbyClient.getApiKey());
  }

  public String postGame(final LobbyGame lobbyGame) {
    return lobbyWatcherClient.postGame(lobbyGame);
  }

  public boolean sendKeepAlive(final String gameId) {
    return lobbyWatcherClient.sendKeepAlive(gameId);
  }

  public void updateGame(final String gameId, final LobbyGame lobbyGame) {
    lobbyWatcherClient.updateGame(gameId, lobbyGame);
  }

  public void disconnect(final String gameId) {
    lobbyWatcherClient.removeGame(gameId);
  }

  public boolean checkConnectivity(final int localPort) {
    return lobbyClient.getConnectivityCheckClient().checkConnectivity(localPort);
  }
}
