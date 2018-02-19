package org.triplea.lobby;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.glassfish.tyrus.client.ClientManager;

@ClientEndpoint
public class SocketClient {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private static CountDownLatch latch;

  @OnOpen
  public void onOpen(final Session session) {
    logger.info("Client Connected ... " + session.getId());
    try {
      session.getBasicRemote().sendText("start");
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @OnMessage
  public String onMessage(final String message, final Session session) {
    logger.info("Client on message Received ...." + message);
    return "client says yes!";
  }

  @OnClose
  public void onClose(final Session session, final CloseReason closeReason) {
    logger.info(String.format("Session %s close because of %s", session.getId(), closeReason));
    latch.countDown();
  }

  public static void main(final String[] args) {
    latch = new CountDownLatch(1);

    final ClientManager client = ClientManager.createClient();
    try {
      client.connectToServer(SocketClient.class, URI.create("ws://localhost:8025/websockets/game"));
      latch.await();
    } catch (DeploymentException | InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
