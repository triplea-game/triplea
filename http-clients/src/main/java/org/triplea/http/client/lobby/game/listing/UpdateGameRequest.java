package org.triplea.http.client.lobby.game.listing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateGameRequest {
  private String gameId;
  private LobbyGame gameData;
}
