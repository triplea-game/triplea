package org.triplea.http.client.lobby.game.lobby.watcher;

import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@Getter
public class GamePostingRequest {
  private LobbyGame lobbyGame;
  private Collection<String> playerNames;
}
