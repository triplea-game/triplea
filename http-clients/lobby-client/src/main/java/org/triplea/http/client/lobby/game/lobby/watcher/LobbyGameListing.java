package org.triplea.http.client.lobby.game.lobby.watcher;

import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.triplea.domain.data.LobbyGame;

/**
 * Data structure representing a game that is registered in the lobby. Lobby tracks games by a
 * 'gameId'.
 */
@Getter
@Builder
@EqualsAndHashCode
@ToString
public class LobbyGameListing {
  @Nonnull private final String gameId;
  @Nonnull private final LobbyGame lobbyGame;
}
