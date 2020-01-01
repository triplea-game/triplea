package org.triplea.server.lobby.game.listing;

import com.google.common.annotations.VisibleForTesting;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.triplea.http.client.lobby.game.listing.GameListingClient;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.server.access.AuthenticatedUser;
import org.triplea.server.http.HttpController;

/** Controller with endpoints for posting, getting and removing games. */
@Builder
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
@RolesAllowed(UserRole.HOST)
public class GameListingController extends HttpController {

  private final GameListing gameListing;

  /** Returns a listing of the current games. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 15, duration = 1, timeUnit = TimeUnit.MINUTES)})
  @GET
  @Path(GameListingClient.FETCH_GAMES_PATH)
  @RolesAllowed(UserRole.ANONYMOUS)
  public Collection<LobbyGameListing> fetchGames() {
    return gameListing.getGames();
  }

  /** Moderator action to remove a game. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.MINUTES)})
  @POST
  @Path(GameListingClient.BOOT_GAME_PATH)
  @RolesAllowed(UserRole.MODERATOR)
  public Response bootGame(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    gameListing.bootGame(authenticatedUser.getUserIdOrThrow(), gameId);
    return Response.ok().build();
  }
}
