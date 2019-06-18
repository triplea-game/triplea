package org.triplea.server.moderator.toolbox.banned.users;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;
import org.triplea.server.moderator.toolbox.api.key.validation.ApiKeyValidationService;

import com.google.common.base.Preconditions;

import lombok.Builder;

/**
 * Controller for endpoints to manage user bans, to be used by moderators.
 */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class UserBanController {
  @Nonnull
  private final UserBanService bannedUsersService;
  @Nonnull
  private final ApiKeyValidationService apiKeyValidationService;

  @GET
  @Path(ToolboxUserBanClient.GET_USER_BANS_PATH)
  public Response getUserBans(@Context final HttpServletRequest request) {
    apiKeyValidationService.verifyApiKey(request);

    return Response
        .ok()
        .entity(bannedUsersService.getBannedUsers())
        .build();
  }

  @POST
  @Path(ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  public Response removeUserBan(@Context final HttpServletRequest request, final String banId) {
    Preconditions.checkArgument(banId != null);
    final int moderatorId = apiKeyValidationService.lookupModeratorIdByApiKey(request);

    final boolean removed = bannedUsersService.removeUserBan(moderatorId, banId);

    return Response
        .status(removed ? 200 : 400)
        .build();
  }

  /**
   * Endpoint to add a user ban. Returns 200 if the ban is added, 400 if not.
   */
  @POST
  @Path(ToolboxUserBanClient.BAN_USER_PATH)
  public Response banUser(@Context final HttpServletRequest request, final UserBanParams banUserParams) {
    Preconditions.checkArgument(banUserParams != null);
    Preconditions.checkArgument(banUserParams.getHashedMac() != null);
    Preconditions.checkArgument(banUserParams.getIp() != null);
    Preconditions.checkArgument(banUserParams.getUsername() != null);
    Preconditions.checkArgument(banUserParams.getHoursToBan() > 0);

    final int moderatorId = apiKeyValidationService.lookupModeratorIdByApiKey(request);

    final boolean banned = bannedUsersService.banUser(moderatorId, banUserParams);

    return Response
        .status(banned ? 200 : 400)
        .build();
  }
}
