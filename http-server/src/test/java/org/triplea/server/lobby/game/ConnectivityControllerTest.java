package org.triplea.server.lobby.game;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.game.ConnectivityCheckClient;
import org.triplea.server.http.ProtectedEndpointTest;

class ConnectivityControllerTest extends ProtectedEndpointTest<ConnectivityCheckClient> {
  private static final int PORT = 20000;

  ConnectivityControllerTest() {
    super(ConnectivityCheckClient::newClient);
  }

  /** Negative case, check connectivity for a port that is not listening. */
  @Test
  void checkConnectivityNegativeCase() {
    final boolean result =
        super.verifyEndpointReturningObject(client -> client.checkConnectivity(PORT));
    assertThat(result, is(false));
  }

  /**
   * Positive case, open a local listening port and verify we can connect to it. This test is to
   * ensure we do not have just a negative case that is trivially true.
   */
  @Test
  void checkConnectivityPositiveCase() throws IOException {
    openSocket();
    final boolean result =
        super.verifyEndpointReturningObject(client -> client.checkConnectivity(PORT));
    assertThat(result, is(true));
  }

  private void openSocket() throws IOException {
    final ServerSocket serverSocket = new ServerSocket(PORT);
    new Thread(
            () -> {
              try {
                final Socket connectedSocket = serverSocket.accept();
                connectedSocket.close();
                serverSocket.close();
              } catch (final IOException e) {
                throw new RuntimeException(e);
              }
            })
        .start();
  }
}
