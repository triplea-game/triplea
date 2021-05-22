package org.triplea.modules.game.lobby.watcher;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConnectivityCheckTest {
  @Mock private Socket socket;

  private ConnectivityCheck connectivityCheck;

  @BeforeEach
  void setUp() {
    connectivityCheck = new ConnectivityCheck(() -> socket);
  }

  @Test
  void connectionSuccess() throws IOException {
    when(socket.isConnected()).thenReturn(true);

    final boolean result = connectivityCheck.canDoReverseConnect("game-host-address", 123);

    assertThat(result, is(true));
    verify(socket).close();
  }

  @Test
  void connectionFailure() throws IOException {
    doThrow(new IOException("simulated exception"))
        .when(socket)
        .connect(eq(new InetSocketAddress("game-host-address", 123)), anyInt());

    final boolean result = connectivityCheck.canDoReverseConnect("game-host-address", 123);

    assertThat(result, is(false));
    verify(socket).close();
  }
}
