package org.triplea.modules.game.listing;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.http.client.web.socket.messages.envelopes.game.listing.LobbyGameRemovedMessage;
import org.triplea.modules.TestData;
import org.triplea.modules.game.listing.GameListing;
import org.triplea.modules.game.listing.GameTtlExpiredListener;
import org.triplea.web.socket.WebSocketMessagingBus;

@ExtendWith(MockitoExtension.class)
class GameTtlExpiredListenerTest {
  @Mock private WebSocketMessagingBus playerMessagingBus;

  @InjectMocks private GameTtlExpiredListener gameTtlExpiredListener;

  @Test
  void verifyGameRemovedCall() {
    final GameListing.GameId gameId = new GameListing.GameId(TestData.API_KEY, "id");

    gameTtlExpiredListener.accept(gameId, TestData.LOBBY_GAME);

    verify(playerMessagingBus).broadcastMessage(new LobbyGameRemovedMessage("id"));
  }
}
