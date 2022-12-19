package org.triplea.spitfire.server.controllers.lobby;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import io.dropwizard.auth.Auth;
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
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.http.HttpStatus;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.dao.user.role.UserRole;
import org.triplea.domain.data.LobbyConstants;
import org.triplea.domain.data.UserName;
import org.triplea.http.client.lobby.game.lobby.watcher.ChatMessageUpload;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingRequest;
import org.triplea.http.client.lobby.game.lobby.watcher.GamePostingResponse;
import org.triplea.http.client.lobby.game.lobby.watcher.LobbyWatcherClient;
import org.triplea.http.client.lobby.game.lobby.watcher.PlayerJoinedNotification;
import org.triplea.http.client.lobby.game.lobby.watcher.PlayerLeftNotification;
import org.triplea.http.client.lobby.game.lobby.watcher.UpdateGameRequest;
import org.triplea.modules.LobbyModuleConfig;
import org.triplea.modules.game.listing.GameListing;
import org.triplea.modules.game.lobby.watcher.ChatUploadModule;
import org.triplea.modules.game.lobby.watcher.GamePostingModule;
import org.triplea.spitfire.server.HttpController;
import org.triplea.spitfire.server.access.authentication.AuthenticatedUser;

/** Controller with endpoints for posting, getting and removing games. */
@Builder
@AllArgsConstructor(
    access = AccessLevel.PACKAGE,
    onConstructor_ = {@VisibleForTesting})
@RolesAllowed(UserRole.HOST)
@Slf4j
public class LobbyWatcherController extends HttpController {
  @VisibleForTesting
  public static final String TEST_ONLY_GAME_POSTING_PATH = "/test-only/lobby/post-game";

  @Nonnull private final Boolean gameHostConnectivityCheckEnabled;
  @Nonnull private final GameListing gameListing;
  @Nonnull private final ChatUploadModule chatUploadModule;
  @Nonnull private final GamePostingModule gamePostingModule;

  public static LobbyWatcherController build(
      final LobbyModuleConfig lobbyModuleConfig, final Jdbi jdbi, final GameListing gameListing) {
    return LobbyWatcherController.builder()
        .gameHostConnectivityCheckEnabled(lobbyModuleConfig.isGameHostConnectivityCheckEnabled())
        .gameListing(gameListing)
        .chatUploadModule(ChatUploadModule.build(jdbi, gameListing))
        .gamePostingModule(GamePostingModule.build(gameListing))
        .build();
  }

  /**
   * Adds a game to the lobby listing. Responds with the gameId assigned to the new game. If we see
   * duplicate posts, the same gameId will be returned.
   */
  @POST
  @Path(LobbyWatcherClient.POST_GAME_PATH)
  public GamePostingResponse postGame(
      @Auth final AuthenticatedUser authenticatedUser,
      final GamePostingRequest gamePostingRequest) {
    Preconditions.checkArgument(gamePostingRequest != null);
    Preconditions.checkArgument(gamePostingRequest.getLobbyGame() != null);

    return gamePostingModule.postGame(authenticatedUser.getApiKey(), gamePostingRequest);
  }

  /**
   * A endpoint available for non-prod-only that allows for integration tests to post games without
   * actually hosting a game themselves (bypasses the reverse connectivity check).
   */
  @POST
  @Path(TEST_ONLY_GAME_POSTING_PATH)
  public Response postGameTestOnly(
      @Auth final AuthenticatedUser authenticatedUser,
      final GamePostingRequest gamePostingRequest) {
    Preconditions.checkArgument(gamePostingRequest != null);
    Preconditions.checkArgument(gamePostingRequest.getLobbyGame() != null);

    return gameHostConnectivityCheckEnabled
        ? Response.status(HttpStatus.NOT_FOUND_404).build()
        : Response.ok()
            .entity(
                GamePostingResponse.builder()
                    .connectivityCheckSucceeded(true)
                    .gameId(gameListing.postGame(authenticatedUser.getApiKey(), gamePostingRequest))
                    .build())
            .build();
  }

  /** Explicit remove of a game from the lobby. */
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
  @POST
  @Path(LobbyWatcherClient.KEEP_ALIVE_PATH)
  public boolean keepAlive(@Auth final AuthenticatedUser authenticatedUser, final String gameId) {
    return gameListing.keepAlive(authenticatedUser.getApiKey(), gameId);
  }

  /** Replaces an existing game with new game data details. */
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

  /** Endpoint used to consume and persist chat messages to database. */
  @POST
  @Path(LobbyWatcherClient.UPLOAD_CHAT_PATH)
  @RolesAllowed(UserRole.HOST)
  public Response uploadChatMessage(
      @Context final HttpServletRequest request, final ChatMessageUpload chatMessageUpload) {
    Preconditions.checkArgument(chatMessageUpload != null);
    Preconditions.checkArgument(chatMessageUpload.getChatMessage() != null);
    Preconditions.checkArgument(chatMessageUpload.getFromPlayer() != null);
    Preconditions.checkArgument(chatMessageUpload.getGameId() != null);

    Preconditions.checkArgument(
        chatMessageUpload.getFromPlayer().length() <= LobbyConstants.USERNAME_MAX_LENGTH);

    String authHeader = request.getHeader("Authorization");

    if (!chatUploadModule.upload(
        // truncate 'Bearer ' from the auth token if present
        authHeader.startsWith("Bearer ") ? authHeader.substring("Bearer ".length()) : authHeader,
        chatMessageUpload)) {
      log.warn(
          "Chat upload request from {} was rejected, "
              + "gameID and API-key pair did not match any existing games.",
          request.getRemoteHost());
    }

    return Response.ok().build();
  }

  @POST
  @Path(LobbyWatcherClient.PLAYER_JOINED_PATH)
  @RolesAllowed(UserRole.HOST)
  public Response playerJoinedGame(
      @Auth final AuthenticatedUser authenticatedUser,
      final PlayerJoinedNotification playerJoinedNotification) {
    gameListing.addPlayerToGame(
        UserName.of(playerJoinedNotification.getPlayerName()),
        authenticatedUser.getApiKey(),
        playerJoinedNotification.getGameId());

    return Response.ok().build();
  }

  @POST
  @Path(LobbyWatcherClient.PLAYER_LEFT_PATH)
  @RolesAllowed(UserRole.HOST)
  public Response playerLeftGame(
      @Auth final AuthenticatedUser authenticatedUser,
      final PlayerLeftNotification playerLeftNotification) {

    gameListing.removePlayerFromGame(
        UserName.of(playerLeftNotification.getPlayerName()),
        authenticatedUser.getApiKey(),
        playerLeftNotification.getGameId());

    return Response.ok().build();
  }
}
