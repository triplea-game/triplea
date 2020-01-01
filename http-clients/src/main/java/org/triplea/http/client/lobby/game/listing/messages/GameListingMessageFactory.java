package org.triplea.http.client.lobby.game.listing.messages;

import lombok.experimental.UtilityClass;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/**
 * Factory class to create {@code ServerMessageEnvelope} instances for each {@code
 * GameListingMessageType}.
 */
@UtilityClass
public class GameListingMessageFactory {
  public static ServerMessageEnvelope gameUpdated(final LobbyGameListing lobbyGameListing) {
    return ServerMessageEnvelope.packageMessage(
        GameListingMessageType.GAME_UPDATED.name(), lobbyGameListing);
  }

  public static ServerMessageEnvelope gameRemoved(final String gameId) {
    return ServerMessageEnvelope.packageMessage(GameListingMessageType.GAME_REMOVED.name(), gameId);
  }
}
