package org.triplea.server.http;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.lobby.chat.LobbyChatClient;

class LobbyWebsocketClientTest extends DropwizardTest {

  private static final int CONNECT_TIMEOUT = 1000;

  @Test
  void verifyConnectivity() throws Exception {
    final URI websocketUri = URI.create(localhost + LobbyChatClient.WEBSOCKET_PATH);

    final org.java_websocket.client.WebSocketClient client =
        new org.java_websocket.client.WebSocketClient(websocketUri) {
          @Override
          public void onOpen(final org.java_websocket.handshake.ServerHandshake serverHandshake) {}

          @Override
          public void onMessage(final String message) {}

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {}

          @Override
          public void onError(final Exception ex) {}
        };

    assertThat(client.isOpen(), is(false));
    client.connect();

    waitForConnection(client);
    client.send("sending! Just to make sure there are no exception here.");

    // small wait to process any responses
    Thread.sleep(10);

    client.close();
    assertThat(client.isOpen(), is(false));
  }

  private void waitForConnection(final org.java_websocket.client.WebSocketClient client)
      throws Exception {
    final long start = System.currentTimeMillis();
    while (!client.isOpen()) {
      Thread.sleep(10);
      final long elapsedMillis = (System.currentTimeMillis() - start);
      if (elapsedMillis > CONNECT_TIMEOUT) {
        fail("Failed to establish websocket connection");
      }
    }
  }
}
