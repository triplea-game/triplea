package games.strategy.engine.lobby.client.ui;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.http.client.web.socket.client.connections.PlayerToLobbyConnection;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameRemovedMessage;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameUpdatedMessage;
import org.triplea.lobby.common.GameDescription;
import org.triplea.lobby.common.LobbyGameUpdateListener;

/**
 * Pure-Java model for the lobby game list. Owns the connection message listeners, the live game
 * list, and all network operations (boot / shutdown-signal). Has no Swing dependency.
 */
class LobbyGameListingModel {
  private final PlayerToLobbyConnection connection;
  private final List<LobbyGameListing> games = new CopyOnWriteArrayList<>();
  private final List<Runnable> changeListeners = new ArrayList<>();

  private final LobbyGameUpdateListener lobbyGameBroadcaster =
      new LobbyGameUpdateListener() {
        @Override
        public void gameUpdated(final LobbyGameListing lobbyGameListing) {
          updateGame(lobbyGameListing);
        }

        @Override
        public void gameRemoved(final String gameId) {
          removeGame(gameId);
        }
      };

  LobbyGameListingModel(final PlayerToLobbyConnection connection) {
    this.connection = connection;

    connection.addMessageListener(
        LobbyGameUpdatedMessage.TYPE,
        msg -> lobbyGameBroadcaster.gameUpdated(msg.getLobbyGameListing()));

    connection.addMessageListener(
        LobbyGameRemovedMessage.TYPE,
        msg -> lobbyGameBroadcaster.gameRemoved(msg.getGameId()));

    connection.fetchGameListing().forEach(lobbyGameBroadcaster::gameUpdated);
  }

  @VisibleForTesting
  LobbyGameUpdateListener getLobbyGameBroadcaster() {
    return lobbyGameBroadcaster;
  }

  int getRowCount() {
    return games.size();
  }

  GameDescription getGameDescriptionForRow(final int i) {
    return GameDescription.fromLobbyGame(games.get(i).getLobbyGame());
  }

  LobbyGameListing getGameListingForRow(final int i) {
    return games.get(i);
  }

  String getGameIdForRow(final int i) {
    return games.get(i).getGameId();
  }

  void bootGame(final String gameId) {
    connection.bootGame(gameId);
  }

  void sendShutdownRequest(final String gameId) {
    connection.sendShutdownRequest(gameId);
  }

  PlayerToLobbyConnection getConnection() {
    return connection;
  }

  void addChangeListener(final Runnable listener) {
    changeListeners.add(listener);
  }

  private void notifyListeners() {
    changeListeners.forEach(Runnable::run);
  }

  private void updateGame(final LobbyGameListing lobbyGameListing) {
    final LobbyGameListing toReplace = findGame(lobbyGameListing.getGameId());
    if (toReplace == null) {
      games.add(lobbyGameListing);
    } else {
      final int replaceIndex = games.indexOf(toReplace);
      games.set(replaceIndex, lobbyGameListing);
    }
    notifyListeners();
  }

  private void removeGame(final String gameId) {
    if (gameId == null) {
      return;
    }
    final LobbyGameListing gameToRemove = findGame(gameId);
    if (gameToRemove != null) {
      games.remove(gameToRemove);
      notifyListeners();
    }
  }

  private LobbyGameListing findGame(final String gameId) {
    return games.stream()
        .filter(game -> game.getGameId().equals(gameId))
        .findFirst()
        .orElse(null);
  }
}

