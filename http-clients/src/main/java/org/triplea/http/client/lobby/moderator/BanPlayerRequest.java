package org.triplea.http.client.lobby.moderator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.triplea.domain.data.PlayerChatId;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class BanPlayerRequest {
  private String playerChatId;
  private long banMinutes;

  public PlayerChatId getPlayerChatId() {
    return PlayerChatId.of(playerChatId);
  }
}
