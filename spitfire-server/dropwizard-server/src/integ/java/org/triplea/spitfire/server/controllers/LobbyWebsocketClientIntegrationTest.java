package org.triplea.spitfire.server.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.net.URI;
import java.time.Duration;
import lombok.AllArgsConstructor;
import org.awaitility.Awaitility;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.web.socket.WebsocketPaths;
import org.triplea.spitfire.server.ControllerIntegrationTest;

@Disabled // Disabled until this can be made more reliable, seeing failure listed below
/*
   ERROR [2020-09-06 01:52:03,515] org.triplea.web.socket.WebSocketMessagingBus: Error-id processing
      websocket message, returning an error message to user. Error id:
      fc66dc4b-61d8-4f87-9444-a00ba418c008
   ! java.nio.channels.ClosedChannelException: null
   ! at org.eclipse.jetty.io.WriteFlusher.onClose(WriteFlusher.java:492)
   ! at org.eclipse.jetty.io.AbstractEndPoint.onClose(AbstractEndPoint.java:353)
   ! at org.eclipse.jetty.io.ChannelEndPoint.onClose(ChannelEndPoint.java:215)
   ! at org.eclipse.jetty.io.AbstractEndPoint.doOnClose(AbstractEndPoint.java:225)
   ! at org.eclipse.jetty.io.AbstractEndPoint.shutdownOutput(AbstractEndPoint.java:157)
   ! at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.disconnect
      (AbstractWebSocketConnection.java:327)
   ! at org.eclipse.jetty.websocket.common.io.DisconnectCallback.failed(DisconnectCallback.java:36)
   ! at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.close
      (AbstractWebSocketConnection.java:200)
   ! at org.eclipse.jetty.websocket.common.io.AbstractWebSocketConnection.onFillable
     (AbstractWebSocketConnection.java:452)
   ! at org.eclipse.jetty.io.AbstractConnection$ReadCallback.succeeded(AbstractConnection.java:305)
   ! at org.eclipse.jetty.io.FillInterest.fillable(FillInterest.java:103)
   ! at org.eclipse.jetty.io.ChannelEndPoint$2.run(ChannelEndPoint.java:117)
   ! at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.runTask(EatWhatYouKill.java:333)
   ! at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.doProduce(EatWhatYouKill.java:310)
   ! at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.tryProduce(EatWhatYouKill.java:168)
   ! at org.eclipse.jetty.util.thread.strategy.EatWhatYouKill.run(EatWhatYouKill.java:126)
   ! at org.eclipse.jetty.util.thread.ReservedThreadExecutor$ReservedThread.run
      (ReservedThreadExecutor.java:366)
   ! at org.eclipse.jetty.util.thread.QueuedThreadPool.runJob(QueuedThreadPool.java:698)
   ! at org.eclipse.jetty.util.thread.QueuedThreadPool$Runner.run(QueuedThreadPool.java:804)
   ! at java.base/java.lang.Thread.run(Thread.java:834)
   INFO  [2020-09-06 01:52:03,521] org.triplea.dropwizard.test.DropwizardServerExtension:
       Running database cleanup..
*/
@AllArgsConstructor
class LobbyWebsocketClientIntegrationTest extends ControllerIntegrationTest {
  private final URI localhost;

  @Test
  @DisplayName("Verify basic websocket operations: open, send, close")
  void verifyConnectivity(final URI host) throws Exception {
    final URI websocketUri = URI.create(host + WebsocketPaths.PLAYER_CONNECTIONS);

    final WebSocketClient client =
        new WebSocketClient(websocketUri) {
          @Override
          public void onOpen(final ServerHandshake serverHandshake) {}

          @Override
          public void onMessage(final String message) {}

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {}

          @Override
          public void onError(final Exception ex) {}
        };

    assertThat(client.isOpen(), is(false));
    client.connect();

    Awaitility.await()
        .pollDelay(Duration.ofMillis(10))
        .atMost(Duration.ofSeconds(1))
        .until(client::isOpen);
    client.send("sending! Just to make sure there are no exception here.");

    // small wait to process any responses
    Thread.sleep(10);

    client.close();
    Awaitility.await()
        .pollDelay(Duration.ofMillis(10))
        .atMost(Duration.ofMillis(100))
        .until(() -> !client.isOpen());
  }
}
