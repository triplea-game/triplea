package org.triplea.lobby.common;

import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;

/**
 * A service that notifies nodes of lobby game state changes (e.g. when games are added to or
 * removed from the lobby).
 */
public interface LobbyGameUpdateListener {
  void gameUpdated(LobbyGameListing lobbyGameListing);

  void gameRemoved(String gameId);
}
