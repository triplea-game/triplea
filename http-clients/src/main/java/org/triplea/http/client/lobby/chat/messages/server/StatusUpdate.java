package org.triplea.http.client.lobby.chat.messages.server;

import lombok.Value;
import org.triplea.domain.data.PlayerName;

@Value
public class StatusUpdate {
  private final PlayerName playerName;
  private final String status;
}
