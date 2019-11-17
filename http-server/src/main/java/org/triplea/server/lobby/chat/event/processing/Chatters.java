package org.triplea.server.lobby.chat.event.processing;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.websocket.Session;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;

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

  Optional<PlayerName> removeSession(final Session session) {
    return Optional.ofNullable(participants.remove(session.getId()))
        .map(ChatterSession::getChatParticipant)
        .map(ChatParticipant::getPlayerName);
  }

  void put(final Session session, final ChatParticipant chatter) {
    participants.put(session.getId(), new ChatterSession(chatter, session));
  }

  Collection<ChatParticipant> getAllParticipants() {
    return participants.values().stream()
        .map(ChatterSession::getChatParticipant)
        .collect(Collectors.toSet());
  }

  public boolean hasPlayer(final PlayerName playerName) {
    return participants.values().stream()
        .map(ChatterSession::getChatParticipant)
        .map(ChatParticipant::getPlayerName)
        .anyMatch(playerName::equals);
  }
}
