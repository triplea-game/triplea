package org.triplea.server.lobby.chat.event.processing;

import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.chat.ChatParticipant;

@Slf4j
@AllArgsConstructor
public class Chatters {
  @AllArgsConstructor
  @Getter
  @VisibleForTesting
  static class ChatterSession {
    private final ChatParticipant chatParticipant;
    private final Session session;
  }

  @Getter(value = AccessLevel.PACKAGE, onMethod_ = @VisibleForTesting)
  private final Map<String, ChatterSession> participants = new HashMap<>();

  Optional<UserName> removeSession(final Session session) {
    return Optional.ofNullable(participants.remove(session.getId()))
        .map(ChatterSession::getChatParticipant)
        .map(ChatParticipant::getUserName);
  }

  void put(final Session session, final ChatParticipant chatter) {
    participants.put(session.getId(), new ChatterSession(chatter, session));
  }

  Collection<ChatParticipant> getAllParticipants() {
    return participants.values().stream()
        .map(ChatterSession::getChatParticipant)
        .collect(Collectors.toSet());
  }

  public boolean hasPlayer(final UserName userName) {
    return participants.values().stream()
        .map(ChatterSession::getChatParticipant)
        .map(ChatParticipant::getUserName)
        .anyMatch(userName::equals);
  }

  public Collection<Session> fetchOpenSessions() {
    return participants.values().stream()
        .map(ChatterSession::getSession)
        .filter(Session::isOpen)
        .findAny()
        .map(Session::getOpenSessions)
        .orElse(Collections.emptySet());
  }

  /**
   * Disconnects all sessions belonging to a given player identified by name. A disconnected session
   * is closed, the closure will trigger a notification on the client of the disconnected player.
   *
   * @param userName The name of the player whose sessions will be disconnected.
   * @param disconnectMessage Message that will be displayed to the disconnected player.
   */
  public void disconnectPlayerSessions(final UserName userName, final String disconnectMessage) {
    final Set<Session> sessions =
        participants.values().stream()
            .filter(
                chatterSession ->
                    chatterSession.getChatParticipant().getUserName().equals(userName))
            .map(ChatterSession::getSession)
            .collect(Collectors.toSet());

    // Do the session disconnects as a second step after gathering sessions to be disconnected.
    // This is to avoid concurrent modification of sessions.
    sessions.forEach(
        session -> {
          try {
            session.close(
                new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, disconnectMessage));
          } catch (final IOException e) {
            log.warn(
                "While closing session, "
                    + "session close threw an exception, session is left open? {}",
                session.isOpen(),
                e);
          }
        });
  }
}
