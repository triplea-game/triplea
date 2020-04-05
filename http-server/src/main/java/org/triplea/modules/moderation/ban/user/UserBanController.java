package org.triplea.modules.moderation.ban.user;

import com.google.common.base.Preconditions;
import es.moki.ratelimij.dropwizard.annotation.Rate;
import es.moki.ratelimij.dropwizard.annotation.RateLimited;
import es.moki.ratelimij.dropwizard.filter.KeyPart;
import io.dropwizard.auth.Auth;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import lombok.Builder;
import org.jdbi.v3.core.Jdbi;
import org.triplea.db.data.UserRole;
import org.triplea.http.HttpController;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.http.client.lobby.moderator.ModeratorChatClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.lobby.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.modules.access.authentication.AuthenticatedUser;
import org.triplea.modules.chat.Chatters;
import org.triplea.web.socket.WebSocketMessagingBus;

/** Controller for endpoints to manage user bans, to be used by moderators. */
@Builder
@RolesAllowed(UserRole.MODERATOR)
public class UserBanController extends HttpController {
  @Nonnull private final UserBanService bannedUsersService;

  public static UserBanController build(
      final Jdbi jdbi,
      final Chatters chatters,
      final WebSocketMessagingBus chatMessagingBus,
      final WebSocketMessagingBus gameMessagingBus) {
    return UserBanController.builder()
        .bannedUsersService(
            UserBanService.builder()
                .jdbi(jdbi)
                .chatters(chatters)
                .chatMessagingBus(chatMessagingBus)
                .gameMessagingBus(gameMessagingBus)
                .build())
        .build();
  }

  @GET
  @Path(ToolboxUserBanClient.GET_USER_BANS_PATH)
  public Response getUserBans() {
    return Response.ok().entity(bannedUsersService.getBannedUsers()).build();
  }

  @POST
  @Path(ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  public Response removeUserBan(
      @Auth final AuthenticatedUser authenticatedUser, final String banId) {
    Preconditions.checkArgument(banId != null);

    final boolean removed =
        bannedUsersService.removeUserBan(authenticatedUser.getUserIdOrThrow(), banId);
    return Response.status(removed ? 200 : 400).build();
  }

  /** Endpoint to add a user ban. Returns 200 if the ban is added, 400 if not. */
  @POST
  @Path(ToolboxUserBanClient.BAN_USER_PATH)
  public Response banUser(
      @Auth final AuthenticatedUser authenticatedUser, final UserBanParams banUserParams) {
    Preconditions.checkArgument(banUserParams != null);
    Preconditions.checkArgument(banUserParams.getSystemId() != null);
    Preconditions.checkArgument(banUserParams.getIp() != null);
    Preconditions.checkArgument(banUserParams.getUsername() != null);
    Preconditions.checkArgument(banUserParams.getMinutesToBan() > 0);

    bannedUsersService.banUser(authenticatedUser.getUserIdOrThrow(), banUserParams);
    return Response.ok().build();
  }

  @POST
  @Path(ModeratorChatClient.BAN_PLAYER_PATH)
  @RateLimited(
      keys = {KeyPart.IP},
      rates = {@Rate(limit = 5, duration = 1, timeUnit = TimeUnit.MINUTES)})
  public Response banPlayer(
      @Auth final AuthenticatedUser authenticatedUser, final BanPlayerRequest banPlayerRequest) {
    Preconditions.checkNotNull(banPlayerRequest);
    Preconditions.checkNotNull(banPlayerRequest.getPlayerChatId());
    Preconditions.checkArgument(banPlayerRequest.getBanMinutes() > 0);

    bannedUsersService.banUser(authenticatedUser.getUserIdOrThrow(), banPlayerRequest);
    return Response.ok().build();
  }
}
