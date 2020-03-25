package org.triplea.modules.game;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

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
  @Mock private InetSocketAddress inetSocketAddress;

  @Mock private Socket socket;

  private ConnectivityCheck connectivityCheck;

  @BeforeEach
  void setup() {
    connectivityCheck = new ConnectivityCheck(() -> socket);
  }

  @Test
  void connectionSuccess() throws IOException {
    final boolean result = connectivityCheck.test(inetSocketAddress);

    assertThat(result, is(true));
    verify(socket).close();
  }

  @Test
  void connectionFailure() throws IOException {
    doThrow(new IOException("simulated exception"))
        .when(socket)
        .connect(eq(inetSocketAddress), anyInt());

    final boolean result = connectivityCheck.test(inetSocketAddress);

    assertThat(result, is(false));
    verify(socket).close();
  }
}
