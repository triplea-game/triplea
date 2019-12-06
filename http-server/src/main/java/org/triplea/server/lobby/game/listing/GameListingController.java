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
import org.triplea.http.client.lobby.game.listing.LobbyGame;
import org.triplea.http.client.lobby.game.listing.LobbyGameListing;
import org.triplea.http.client.lobby.game.listing.UpdateGameRequest;
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

  /**
   * Adds a game to the lobby listing. Responds with the gameId assigned to the new game. If we see
   * duplicate posts, the same gameId will be returned.
   */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 20, duration = 4, timeUnit = TimeUnit.MINUTES)})
  @POST
  @Path(GameListingClient.POST_GAME_PATH)
  public String postGame(
      @Auth final AuthenticatedUser authenticatedUser, final LobbyGame lobbyGame) {
    // TODO: Project#12 Combine availability check with gameListing.postGame
    return gameListing.postGame(authenticatedUser.getApiKey(), lobbyGame);
  }

  /** Explicit remove of a game from the lobby. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @POST
  @Path(GameListingClient.REMOVE_GAME_PATH)
  public Response removeGame(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    gameListing.removeGame(authenticatedUser.getApiKey(), gameId);
    return Response.ok().build();
  }

  /**
   * "Alive" endpoint to periodically invoked after a game has been posted to indicate the client is
   * still hosting and is alive. If the endpoint is not invoked within a cutoff time then the game
   * with the corresponding gameId will be unlisted. The return value indicates if the game has been
   * kept alive, or false indicates the game was already removed and the client should re-post.
   */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @POST
  @Path(GameListingClient.KEEP_ALIVE_PATH)
  public boolean keepAlive(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    return gameListing.keepAlive(authenticatedUser.getApiKey(), gameId);
  }

  /** Returns a listing of the current games. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @GET
  @Path(GameListingClient.FETCH_GAMES_PATH)
  @RolesAllowed(UserRole.ANONYMOUS)
  public Collection<LobbyGameListing> fetchGames() {
    return gameListing.getGames();
  }

  /** Replaces an existing game with new game data details. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @POST
  @Path(GameListingClient.UPDATE_GAME_PATH)
  public Response updateGame(
      @Auth final AuthenticatedUser authenticatedUser, final UpdateGameRequest updateGameRequest) {
    gameListing.updateGame(
        authenticatedUser.getApiKey(),
        updateGameRequest.getGameId(),
        updateGameRequest.getGameData());
    return Response.ok().build();
  }

  /** Moderator action to remove a game. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @POST
  @Path(GameListingClient.BOOT_GAME_PATH)
  @RolesAllowed(UserRole.MODERATOR)
  public Response bootGame(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    gameListing.bootGame(authenticatedUser.getUserIdOrThrow(), gameId);
    return Response.ok().build();
  }
}
