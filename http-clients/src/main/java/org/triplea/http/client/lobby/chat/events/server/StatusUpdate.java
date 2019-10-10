package org.triplea.http.client.lobby.chat.events.server;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.triplea.domain.data.PlayerName;

@AllArgsConstructor
@Getter
public class StatusUpdate {
  private final PlayerName playerName;
  private final String status;
}
