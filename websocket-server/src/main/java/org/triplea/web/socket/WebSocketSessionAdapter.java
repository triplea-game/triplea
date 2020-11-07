package org.triplea.web.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import lombok.experimental.UtilityClass;
import org.java_websocket.WebSocket;
import org.slf4j.LoggerFactory;

/**
 * Converts 'session' objects that we receive as parameters to websocket servers to implementations
 * of the unified interface 'WebSocketSession'.
 */
@UtilityClass
public class WebSocketSessionAdapter {
  static WebSocketSession fromSession(final Session session) {
    return new WebSocketSession() {
      @Override
      public boolean isOpen() {
        return session.isOpen();
      }

      @Override
      public InetAddress getRemoteAddress() {
        return InetExtractor.extract(session.getUserProperties());
      }

      @Override
      public void close(final CloseReason closeReason) {
        try {
          session.close(closeReason);
        } catch (final IOException e) {
          LoggerFactory.getLogger(WebSocketSessionAdapter.class)
              .warn("Error closing websocket session", e);
        }
      }

      @Override
      public void sendText(final String text) {
        try {
          session.getAsyncRemote().sendText(text).get();
        } catch (final InterruptedException | ExecutionException e) {
          LoggerFactory.getLogger(WebSocketSessionAdapter.class)
              .error("Error sending websocket message", e);
        }
      }

      @Override
      public String getId() {
        return session.getId();
      }
    };
  }

  static WebSocketSession fromWebSocket(final WebSocket webSocket) {
    return new WebSocketSession() {
      private final String id = UUID.randomUUID().toString();

      @Override
      public boolean isOpen() {
        return webSocket.isOpen();
      }

      @Override
      public InetAddress getRemoteAddress() {
        return webSocket.getRemoteSocketAddress().getAddress();
      }

      @Override
      public void close(final CloseReason closeReason) {
        webSocket.close(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
      }

      @Override
      public void sendText(final String text) {
        webSocket.send(text);
      }

      @Override
      public String getId() {
        return id;
      }
    };
  }
}
