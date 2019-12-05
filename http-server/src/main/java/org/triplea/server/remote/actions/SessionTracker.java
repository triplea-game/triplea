package org.triplea.server.remote.actions;

import java.net.InetAddress;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.websocket.Session;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Builder
@Slf4j
class SessionTracker {
  private final Map<InetAddress, Map<String, Session>> listeningSessions = new HashMap<>();

  @Nonnull private final Function<Session, InetAddress> ipAddressExtractor;

  void addSession(final Session session) {
    final InetAddress ip = ipAddressExtractor.apply(session);
    log.info("Adding websocket session, IP: {}, session id: {}", ip, session.getId());

    if (!listeningSessions.containsKey(ip)) {
      listeningSessions.put(ip, new HashMap<>());
    }
    listeningSessions.computeIfAbsent(ip, k -> new HashMap<>()).put(session.getId(), session);
  }

  void removeSession(final Session session) {
    final InetAddress ip = ipAddressExtractor.apply(session);
    log.info("Remove websocket session, IP: {}, session id: {}", ip, session.getId());

    if (listeningSessions.containsKey(ip)) {
      listeningSessions.get(ip).remove(session.getId());
      if (listeningSessions.get(ip).isEmpty()) {
        listeningSessions.remove(ip);
      }
    }
  }

  /** Any closed sessions are removed and returns all open sessions. */
  Collection<Session> getSessions() {
    final Collection<Session> closedSessions =
        listeningSessions.values().stream()
            .flatMap(sessions -> sessions.values().stream())
            .filter(Predicate.not(Session::isOpen))
            .collect(Collectors.toSet());

    closedSessions.forEach(this::removeSession);

    return listeningSessions.values().stream()
        .flatMap(sessions -> sessions.values().stream())
        .collect(Collectors.toSet());
  }

  Collection<Session> getSessionsByIp(final InetAddress ip) {
    return listeningSessions.getOrDefault(ip, Map.of()).values();
  }
}
