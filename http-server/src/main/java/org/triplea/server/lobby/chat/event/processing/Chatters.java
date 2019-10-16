package org.triplea.server.lobby.chat.event.processing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.websocket.Session;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;

// TODO: test-me
class Chatters {
  private final Map<String, ChatParticipant> participants = new HashMap<>();

  Optional<PlayerName> remove(final Session session) {
    return Optional.ofNullable(participants.remove(session.getId()))
        .map(ChatParticipant::getPlayerName);
  }

  void put(final Session session, final ChatParticipant chatter) {
    participants.put(session.getId(), chatter);
  }

  Collection<ChatParticipant> values() {
    return participants.values();
  }
}
