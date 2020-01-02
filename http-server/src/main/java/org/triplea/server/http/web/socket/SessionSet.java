package org.triplea.server.http.web.socket;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.server.lobby.chat.InetExtractor;

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
@Slf4j
public class SessionSet {

  @Getter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

  public void put(final Session session) {
    sessions.add(session);
  }

  public void remove(final Session session) {
    sessions.remove(session);
  }

  public Collection<Session> values() {
    return sessions.stream().filter(Session::isOpen).collect(Collectors.toSet());
  }

  public Collection<Session> getSessionsByIp(final InetAddress serverIp) {
    return sessions.stream()
        .filter(Session::isOpen)
        .filter(s -> InetExtractor.extract(s.getUserProperties()).equals(serverIp))
        .collect(Collectors.toSet());
  }

  /** All sessions matching a given IP are closed. */
  public void closeSessionsByIp(final InetAddress ip) {
    sessions.stream()
        .filter(Session::isOpen)
        .filter(s -> InetExtractor.extract(s.getUserProperties()).equals(ip))
        .forEach(
            s -> {
              try {
                s.close();
              } catch (final IOException e) {
                log.warn("Could not close session (session is still open? {}", s.isOpen(), e);
              }
            });
  }
}
