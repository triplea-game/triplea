package org.triplea.http.client.lobby.game.listing;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

/**
 * Data structure representing a game that is registered in the lobby. Lobby tracks games by a
 * 'gameId'.
 */
@Getter
@Builder
@EqualsAndHashCode
public class LobbyGameListing {
  @NonNull private final String gameId;
  @NonNull private final LobbyGame lobbyGame;
}
