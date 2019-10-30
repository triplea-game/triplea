package org.triplea.http.client.lobby.chat.events.server;

import java.util.List;
import lombok.Value;
import org.triplea.http.client.lobby.chat.ChatParticipant;

@Value
public class PlayerListing {
  private final List<ChatParticipant> chatters;
}
