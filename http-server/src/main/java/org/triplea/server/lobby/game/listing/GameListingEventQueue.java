package org.triplea.server.lobby.game.listing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.websocket.Session;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageFactory;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Receives game listing events and dispatches event messages to listeners. */
// TODO: test-me
@Builder
@RequiredArgsConstructor
public class GameListingEventQueue {
  private final BiConsumer<Collection<Session>, ServerMessageEnvelope> broadcaster;
  private final Map<String, Session> sessions = new HashMap<>();

  void addListener(final Session session) {
    sessions.put(session.getId(), session);
  }

  void removeListener(final String sessionId) {
    sessions.remove(sessionId);
  }

  void gameRemoved(final String gameId) {
    removeClosedSessions();
    broadcaster.accept(sessions.values(), GameListingMessageFactory.gameRemoved(gameId));
  }

  void gameUpdated(final LobbyGameListing gameListing) {
    removeClosedSessions();
    broadcaster.accept(sessions.values(), GameListingMessageFactory.gameUpdated(gameListing));
  }

  /** Just in case we fail to remove a closed session from listeners, remove any closed sessions. */
  private void removeClosedSessions() {
    // find all sessions that are not open
    final Set<Session> closedSessions =
        sessions.values().stream()
            .filter(Predicate.not(Session::isOpen))
            .collect(Collectors.toSet());
    // remove each of the closed sessions
    closedSessions.stream().map(Session::getId).forEach(sessions::remove);
  }
}
