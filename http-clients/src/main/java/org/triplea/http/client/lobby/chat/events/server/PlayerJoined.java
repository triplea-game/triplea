package org.triplea.http.client.lobby.chat.events.server;

import lombok.Value;
import org.triplea.http.client.lobby.chat.ChatParticipant;

@Value
public class PlayerJoined {
  private final ChatParticipant chatParticipant;
}
