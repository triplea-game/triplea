package org.triplea.web.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.slf4j.LoggerFactory;

public interface WebSocketSession {
  static WebSocketSession fromSession(Session session) {
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
      public void close() {
        close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Session closed by server"));
      }

      @Override
      public void close(final CloseReason closeReason) {
        try {
          session.close(closeReason);
        } catch (final IOException e) {
          LoggerFactory.getLogger(WebSocketSession.class)
              .warn("Error closing websocket session", e);
        }
      }

      @Override
      public void sendText(final String text) {
        try {
          session.getAsyncRemote().sendText(text).get();
        } catch (final InterruptedException | ExecutionException e) {
          LoggerFactory.getLogger(WebSocketSession.class)
              .error("Error sending websocket message", e);
        }
      }

      @Override
      public String getId() {
        return session.getId();
      }
    };
  }

  boolean isOpen();

  InetAddress getRemoteAddress();

  void close();

  void close(CloseReason closeReason);

  void sendText(String text);

  String getId();
}
