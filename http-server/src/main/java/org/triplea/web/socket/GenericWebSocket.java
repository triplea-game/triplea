package org.triplea.web.socket;

import static org.triplea.web.socket.WebSocketMessagingBus.MESSAGING_BUS_KEY;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import lombok.experimental.UtilityClass;

/**
 * Extracts common code between websocket server methods. Each websocket endpoint is essentially
 * identical and they delegate their behavior to a 'messagingBus' which has listeners that will act
 * on messages received. In general there should not be many websocket endpoints and they are
 * grouped by the "types" of connections that are created to them (eg: players or games)
 */
@UtilityClass
public class GenericWebSocket {
  public static void onOpen(final Session session) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY)).onOpen(session);
  }

  public static void onMessage(final Session session, final String message) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY))
        .onMessage(session, message);
  }

  public static void onClose(final Session session, final CloseReason closeReason) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY)).onClose(session);
  }

  public static void onError(final Session session, final Throwable throwable) {
    ((WebSocketMessagingBus) session.getUserProperties().get(MESSAGING_BUS_KEY))
        .onError(session, throwable);
  }
}
