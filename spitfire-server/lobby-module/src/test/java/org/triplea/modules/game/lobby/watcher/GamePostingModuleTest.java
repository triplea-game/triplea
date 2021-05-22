package org.triplea.modules.game.lobby.watcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.modules.TestData;
import org.triplea.modules.game.listing.GameListing;

@ExtendWith(MockitoExtension.class)
class GamePostingModuleTest {
  private static final GamePostingRequest gamePostingRequest =
      GamePostingRequest.builder().lobbyGame(TestData.LOBBY_GAME).build();

  @Mock private GameListing gameListing;
  @Mock private ConnectivityCheck connectivityCheck;

  @InjectMocks private GamePostingModule gamePostingModule;

  @Test
  void connectivityCheckSucceeds() {
    when(connectivityCheck.canDoReverseConnect(
            TestData.LOBBY_GAME.getHostAddress(), TestData.LOBBY_GAME.getHostPort()))
        .thenReturn(true);
    when(gameListing.postGame(ApiKey.of("api-key"), gamePostingRequest)).thenReturn("game-id");

    final GamePostingResponse gamePostingResponse =
        gamePostingModule.postGame(ApiKey.of("api-key"), gamePostingRequest);

    assertThat(gamePostingResponse.isConnectivityCheckSucceeded(), is(true));
    assertThat(gamePostingResponse.getGameId(), is("game-id"));
  }

  @Test
  void connectivityCheckFails() {
    when(connectivityCheck.canDoReverseConnect(
            TestData.LOBBY_GAME.getHostAddress(), TestData.LOBBY_GAME.getHostPort()))
        .thenReturn(false);

    final GamePostingResponse gamePostingResponse =
        gamePostingModule.postGame(ApiKey.of("api-key"), gamePostingRequest);

    assertThat(gamePostingResponse.isConnectivityCheckSucceeded(), is(false));
    assertThat(gamePostingResponse.getGameId(), is(nullValue()));
    verify(gameListing, never()).postGame(any(), any());
  }
}
