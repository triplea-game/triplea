package org.triplea.http.client.lobby.chat.messages.server;

import java.util.List;
import lombok.Value;
import org.triplea.http.client.lobby.chat.ChatParticipant;

@Value
public class ChatterList {
  private final List<ChatParticipant> chatters;
}
