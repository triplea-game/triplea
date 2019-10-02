package org.triplea.lobby.common;

import org.triplea.http.client.lobby.game.listing.LobbyGame;

/**
 * A service that notifies nodes of lobby game state changes (e.g. when games are added to or
 * removed from the lobby).
 */
public interface LobbyGameUpdateListener {
  void gameUpdated(String gameId, LobbyGame description);

  void gameRemoved(String gameId);
}
