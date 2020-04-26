package org.triplea.modules.game.lobby.watcher;

import java.util.function.BiConsumer;
import lombok.AllArgsConstructor;
import org.triplea.domain.data.LobbyGame;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameRemovedMessage;
import org.triplea.web.socket.WebSocketMessagingBus;

@AllArgsConstructor
class GameTtlExpiredListener implements BiConsumer<GameListing.GameId, LobbyGame> {

  private final WebSocketMessagingBus playerMessagingBus;

  @Override
  public void accept(final GameListing.GameId gameId, final LobbyGame removedEntry) {
    playerMessagingBus.broadcastMessage(new LobbyGameRemovedMessage(gameId.getId()));
  }
}
