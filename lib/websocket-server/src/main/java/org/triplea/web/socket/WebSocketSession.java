package org.triplea.web.socket;

import java.net.InetAddress;
import javax.websocket.CloseReason;

public interface WebSocketSession {
  boolean isOpen();

  InetAddress getRemoteAddress();

  default void close() {
    close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Session closed by server"));
  }

  void close(CloseReason closeReason);

  void sendText(String text);

  String getId();
}
