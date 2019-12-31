package org.triplea.server.lobby.game.listing;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.messages.GameListingMessageFactory;
import org.triplea.http.client.web.socket.messages.ServerMessageEnvelope;

/** Receives game listing events and dispatches event messages to listeners. */
@Builder
@RequiredArgsConstructor
public class GameListingEventQueue {
  private final BiConsumer<Collection<Session>, ServerMessageEnvelope> broadcaster;

  @Getter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private final Set<Session> sessions = new HashSet<>();

  void addListener(final Session session) {
    Preconditions.checkNotNull(session);
    sessions.add(session);
  }

  void removeListener(final Session session) {
    sessions.remove(session);
  }

  void gameRemoved(final String gameId) {
    removeClosedSessions();
    broadcaster.accept(sessions, GameListingMessageFactory.gameRemoved(gameId));
  }

  void gameUpdated(final LobbyGameListing gameListing) {
    removeClosedSessions();
    broadcaster.accept(sessions, GameListingMessageFactory.gameUpdated(gameListing));
  }

  /** Just in case we fail to remove a closed session from listeners, remove any closed sessions. */
  private void removeClosedSessions() {
    // find all sessions that are not open and remove
    sessions.removeIf(Predicate.not(Session::isOpen));
  }
}
