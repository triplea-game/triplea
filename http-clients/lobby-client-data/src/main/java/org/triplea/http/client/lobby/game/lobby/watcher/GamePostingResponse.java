package org.triplea.http.client.lobby.game.lobby.watcher;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class GamePostingResponse {
  private final String gameId;
  private final boolean connectivityCheckSucceeded;
}
