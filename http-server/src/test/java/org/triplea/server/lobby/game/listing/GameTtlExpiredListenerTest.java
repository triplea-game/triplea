package org.triplea.server.lobby.game.listing;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.java.cache.TtlCache;
import org.triplea.server.TestData;
import org.triplea.server.lobby.game.listing.GameListing.GameId;

@ExtendWith(MockitoExtension.class)
class GameTtlExpiredListenerTest {
  @Mock private GameListingEventQueue gameListingEventQueue;

  @InjectMocks private GameTtlExpiredListener gameTtlExpiredListener;

  @Test
  void verifyGameRemovedCall() {
    final GameId gameId = new GameId(TestData.API_KEY, "id");

    gameTtlExpiredListener.accept(gameId, TestData.LOBBY_GAME);

    verify(gameListingEventQueue).gameRemoved("id");
  }
}
