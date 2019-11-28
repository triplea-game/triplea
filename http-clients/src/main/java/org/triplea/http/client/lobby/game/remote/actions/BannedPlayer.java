package org.triplea.http.client.lobby.game.remote.actions;

import lombok.Builder;
import lombok.Value;
import org.triplea.domain.data.PlayerName;

@Value
@Builder
public class BannedPlayer {
  private String playerName;
  private String ipAddress;

  public PlayerName getPlayerName() {
    return PlayerName.of(playerName);
  }
}
