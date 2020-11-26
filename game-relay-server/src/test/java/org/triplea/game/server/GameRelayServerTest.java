package org.triplea.game.server;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.web.socket.GenericWebSocketClient;

/**
 * The relay server should respond back with any messages we send it. To test we set up clients that
 * add received messages to a list. We then send a message from a client and then verify that all
 * connected clients get a message back by checking the received messages list.
 */
class GameRelayServerTest {
  // Use a random port between 6000 and 7000. It takes time to shut down the relay
  // server, if we re-run this test quickly we'll have issues starting the server
  // up on the same port.
  private static final int port = 6000 + ((int) (Math.random() * 1000));
  private static final URI SERVER_URI = URI.create("ws://localhost:" + port);
  private static final GameRelayServer gameRelayServer = new GameRelayServer(port);

  @AfterAll
  static void stopServer() {
    gameRelayServer.stop();
  }

  @Test
  @DisplayName(
      "Send a message with a single client to relay server and "
          + "verify we received the same message back")
  void singleClient() {
    final List<SampleMessage> receivedMessages = new ArrayList<>();
    final GenericWebSocketClient webSocketClient =
        createClientWithReceivedMessageQueue(receivedMessages);

    webSocketClient.sendMessage(new SampleMessage("test message"));

    Awaitility.await().atMost(3, TimeUnit.SECONDS).until(() -> !receivedMessages.isEmpty());
    assertThat(receivedMessages.get(0).getContents(), is("test message"));
    assertThat(receivedMessages, hasSize(1));
  }

  private GenericWebSocketClient createClientWithReceivedMessageQueue(
      final Collection<SampleMessage> receivedMessageQueue) {

    final GenericWebSocketClient webSocketClient = new GenericWebSocketClient(SERVER_URI);
    webSocketClient.addListener(SampleMessage.TYPE, receivedMessageQueue::add);
    webSocketClient.connect();
    return webSocketClient;
  }

  @Test
  @DisplayName(
      "Connect with multiple clients, send a message once and "
          + "verify each client receives a message")
  void multipleClients() {
    final List<SampleMessage> receivedMessages1 = new ArrayList<>();
    final GenericWebSocketClient webSocketClient1 =
        createClientWithReceivedMessageQueue(receivedMessages1);

    final List<SampleMessage> receivedMessages2 = new ArrayList<>();
    createClientWithReceivedMessageQueue(receivedMessages2);

    final List<SampleMessage> receivedMessages3 = new ArrayList<>();
    createClientWithReceivedMessageQueue(receivedMessages3);

    webSocketClient1.sendMessage(new SampleMessage("test message"));

    Awaitility.await()
        .atMost(3, TimeUnit.SECONDS)
        .until(
            () ->
                !receivedMessages1.isEmpty()
                    && !receivedMessages2.isEmpty()
                    && !receivedMessages3.isEmpty());

    assertThat(receivedMessages1, hasSize(1));
    assertThat(receivedMessages2, hasSize(1));
    assertThat(receivedMessages3, hasSize(1));
  }
}
