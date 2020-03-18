package org.triplea.http.client.lobby.game.listing;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
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
  @NonNull private final String gameId;
  @NonNull private final LobbyGame lobbyGame;
}
