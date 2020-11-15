package org.triplea.web.socket;

import java.net.InetSocketAddress;
import javax.websocket.CloseReason;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class StandaloneWebsocketServer extends WebSocketServer {
  private final GenericWebSocket genericWebSocket;

  public StandaloneWebsocketServer(final GenericWebSocket genericWebSocket) {
    super(new InetSocketAddress(5001));
    this.genericWebSocket = genericWebSocket;
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
