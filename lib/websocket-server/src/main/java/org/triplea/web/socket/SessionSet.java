package org.triplea.web.socket;

import com.google.common.annotations.VisibleForTesting;
import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * Tracks sessions, can be used to listen to IP session ban events and close any matching sessions.
 *
 * <p>Closed sessions are removed when sessions are retrieved, so there is no need to 'remove' or
 * 'unregister' sessions.
 *
 * <ol>
 *   This class has two main purposes:
 *   <li>Unify common session tracking code
 *   <li>Provide a mechanism to close already established connections that have been banned. We will
 *       rely on request filtering to prevent banned sessions from creating a new websocket
 *       connection, this part is needed to remove existing connections of a banned user.
 * </ol>
 */
public class SessionSet {

  @Getter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private final Collection<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

  //  private final MessageSender messageSender = new MessageSender();
  //  private final MessageBroadcaster messageBroadcaster = new MessageBroadcaster(messageSender);

  public void put(final WebSocketSession session) {
    sessions.add(session);
  }

  public void remove(final WebSocketSession session) {
    sessions.remove(session);
  }

  public Collection<WebSocketSession> values() {
    return sessions.stream().filter(WebSocketSession::isOpen).collect(Collectors.toSet());
  }

  public Collection<WebSocketSession> getSessionsByIp(final InetAddress serverIp) {
    return sessions.stream()
        .filter(WebSocketSession::isOpen)
        .filter(s -> s.getRemoteAddress().equals(serverIp))
        .collect(Collectors.toSet());
  }

  /** All sessions matching a given IP are closed. */
  public void closeSessionsByIp(final InetAddress ip) {
    sessions.stream()
        .filter(WebSocketSession::isOpen)
        .filter(s -> s.getRemoteAddress().equals(ip))
        .forEach(WebSocketSession::close);
  }
}
