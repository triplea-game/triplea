package org.triplea.modules.game.lobby.watcher;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.data.UserRole;
import org.triplea.domain.data.ApiKey;
import org.triplea.domain.data.LobbyGame;
import org.triplea.domain.data.UserName;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.http.client.lobby.game.lobby.watcher.UpdateGameRequest;
import org.triplea.modules.access.authentication.AuthenticatedUser;

/** Controller with endpoints for posting, getting and removing games. */
@Builder
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
@RolesAllowed(UserRole.HOST)
public class LobbyWatcherController extends HttpController {
  @Nonnull private final GameListing gameListing;
  @Nonnull private final Consumer<ChatMessageUpload> chatUploadModule;

  public static LobbyWatcherController build(final Jdbi jdbi, final GameListing gameListing) {
    return LobbyWatcherController.builder()
        .gameListing(gameListing)
        .chatUploadModule(ChatUploadModule.build(jdbi, gameListing))
        .build();
  }

  /**
   * Adds a game to the lobby listing. Responds with the gameId assigned to the new game. If we see
   * duplicate posts, the same gameId will be returned.
   */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 20, duration = 4, timeUnit = TimeUnit.MINUTES)})
  @POST
  @Path(LobbyWatcherClient.POST_GAME_PATH)
  public String postGame(
      @Auth final AuthenticatedUser authenticatedUser, final LobbyGame lobbyGame) {
    return gameListing.postGame(authenticatedUser.getApiKey(), lobbyGame);
  }

  /** Explicit remove of a game from the lobby. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @POST
  @Path(LobbyWatcherClient.REMOVE_GAME_PATH)
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
  @Path(LobbyWatcherClient.KEEP_ALIVE_PATH)
  public boolean keepAlive(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    return gameListing.keepAlive(authenticatedUser.getApiKey(), gameId);
  }

  /** Replaces an existing game with new game data details. */
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @POST
  @Path(LobbyWatcherClient.UPDATE_GAME_PATH)
  public Response updateGame(
      @Auth final AuthenticatedUser authenticatedUser, final UpdateGameRequest updateGameRequest) {
    gameListing.updateGame(
        authenticatedUser.getApiKey(),
        updateGameRequest.getGameId(),
        updateGameRequest.getGameData());
    return Response.ok().build();
  }

  @POST
  @Path(LobbyWatcherClient.UPLOAD_CHAT_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 10, duration = 1, timeUnit = TimeUnit.SECONDS)})
  @RolesAllowed(UserRole.HOST)
  public Response uploadChatMessage(
      @Context final HttpServletRequest request, final ChatMessageUpload chatMessageUpload) {
    Preconditions.checkArgument(chatMessageUpload != null);
    Preconditions.checkArgument(chatMessageUpload.getChatMessage() != null);
    Preconditions.checkArgument(chatMessageUpload.getFromPlayer() != null);
    Preconditions.checkArgument(chatMessageUpload.getGameId() != null);
    Preconditions.checkArgument(chatMessageUpload.getApiKey() != null);

    Preconditions.checkArgument(chatMessageUpload.getFromPlayer().length() <= UserName.MAX_LENGTH);
    Preconditions.checkArgument(chatMessageUpload.getApiKey().length() <= ApiKey.MAX_LENGTH);

    chatUploadModule.accept(chatMessageUpload);

    return Response.ok().build();
  }
}
