package org.triplea.modules.game;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.ApiKey;
import org.triplea.modules.game.listing.GameListing;

@ExtendWith(MockitoExtension.class)
class ConnectivityCheckTest {
  @Mock private Socket socket;

  @Mock private GameListing gameListing;

  private ConnectivityCheck connectivityCheck;

  @BeforeEach
  void setup() {
    connectivityCheck = new ConnectivityCheck(() -> socket, gameListing);
  }

  @Test
  void gameIdNotFound() {
    when(gameListing.getHostForGame(ApiKey.of("api-key"), "game-id")).thenReturn(Optional.empty());

    final boolean result = connectivityCheck.canDoReverseConnect(ApiKey.of("api-key"), "game-id");

    assertThat(result, is(false));
  }

  @Test
  void connectionSuccess() throws IOException {
    when(gameListing.getHostForGame(ApiKey.of("api-key"), "game-id"))
        .thenReturn(Optional.of(new InetSocketAddress(3300)));

    final boolean result = connectivityCheck.canDoReverseConnect(ApiKey.of("api-key"), "game-id");

    assertThat(result, is(true));
    verify(socket).close();
  }

  @Test
  void connectionFailure() throws IOException {
    when(gameListing.getHostForGame(ApiKey.of("api-key"), "game-id"))
        .thenReturn(Optional.of(new InetSocketAddress(3300)));

    doThrow(new IOException("simulated exception"))
        .when(socket)
        .connect(eq(new InetSocketAddress(3300)), anyInt());

    final boolean result = connectivityCheck.canDoReverseConnect(ApiKey.of("api-key"), "game-id");

    assertThat(result, is(false));
    verify(socket).close();
  }
}
