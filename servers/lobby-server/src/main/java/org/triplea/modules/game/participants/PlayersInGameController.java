package org.triplea.modules.game.participants;

import com.google.common.base.Preconditions;
import io.dropwizard.auth.Auth;
import java.util.Collection;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import lombok.AllArgsConstructor;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.player.PlayerLobbyActionsClient;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.game.listing.GameListing;

@RolesAllowed(UserRole.ANONYMOUS)
@AllArgsConstructor
public class PlayersInGameController extends HttpController {
  private final GameListing gameListing;

  public static PlayersInGameController build(final GameListing gameListing) {
    return new PlayersInGameController(gameListing);
  }

  @POST
  @Path(PlayerLobbyActionsClient.FETCH_PLAYERS_IN_GAME)
  public Collection<String> fetchPlayersInGame(
      @Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    Preconditions.checkNotNull(gameId);
    return gameListing.getPlayersInGame(gameId);
  }
}
