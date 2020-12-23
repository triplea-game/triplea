package org.triplea.modules.game.listing;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.auth.Auth;
import java.util.Collection;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.game.lobby.watcher.GameListingClient;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyGameListing;
import org.triplea.modules.access.authentication.AuthenticatedUser;

/** Controller with endpoints for posting, getting and removing games. */
@Builder
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
@RolesAllowed(UserRole.HOST)
public class GameListingController extends HttpController {

  private final GameListing gameListing;

  public static GameListingController build(final GameListing gameListing) {
    return GameListingController.builder() //
        .gameListing(gameListing)
        .build();
  }

  /** Returns a listing of the current games. */
  @GET
  @Path(GameListingClient.FETCH_GAMES_PATH)
  @RolesAllowed(UserRole.ANONYMOUS)
  public Collection<LobbyGameListing> fetchGames() {
    return gameListing.getGames();
  }

  /** Moderator action to remove a game. */
  @POST
  @Path(GameListingClient.BOOT_GAME_PATH)
  @RolesAllowed(UserRole.MODERATOR)
  public Response bootGame(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    gameListing.bootGame(authenticatedUser.getUserIdOrThrow(), gameId);
    return Response.ok().build();
  }
}
