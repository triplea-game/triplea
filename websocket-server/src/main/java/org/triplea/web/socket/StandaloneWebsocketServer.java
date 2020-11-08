package org.triplea.web.socket;

import java.net.InetSocketAddress;
import javax.websocket.CloseReason;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.triplea.http.client.web.socket.messages.envelopes.chat.ChatSentMessage;

@Slf4j
public class StandaloneWebsocketServer extends WebSocketServer {
  private final GenericWebSocket genericWebSocket;

  public StandaloneWebsocketServer(final GenericWebSocket genericWebSocket) {
    super(new InetSocketAddress(5001));
    this.genericWebSocket = genericWebSocket;
  }

  // temporary main code used for demo purposes.
  public static void main(final String[] args) {
    final WebSocketMessagingBus messagingBus =
        new WebSocketMessagingBus(
            new MessageBroadcaster(new MessageSender()), new MessageSender(), new SessionSet());

    messagingBus.addListener(
        ChatSentMessage.TYPE, messageContext -> log.info("received: " + messageContext.message));
    final StandaloneWebsocketServer standaloneWebsocketServer =
        new StandaloneWebsocketServer(new GenericWebSocket(messagingBus, (ip) -> false));
    standaloneWebsocketServer.start();
  }

  @Override
  public void onOpen(final WebSocket webSocket, final ClientHandshake handshake) {
    genericWebSocket.onOpen(webSocket);
  }

  @Override
  public void onClose(
      final WebSocket webSocket, final int code, final String reason, final boolean remote) {
    genericWebSocket.onClose(webSocket, new CloseReason(() -> code, reason));
  }

  @Override
  public void onMessage(final WebSocket webSocket, final String message) {
    genericWebSocket.onMessage(webSocket, message);
  }

  @Override
  public void onError(final WebSocket webSocket, final Exception exception) {
    genericWebSocket.onError(webSocket, exception);
  }

  @Override
  public void onStart() {}
}
