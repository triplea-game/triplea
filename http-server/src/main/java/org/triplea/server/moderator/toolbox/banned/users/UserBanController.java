package org.triplea.server.moderator.toolbox.banned.users;

import com.google.common.base.Preconditions;
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
import lombok.Builder;
import org.triplea.http.client.moderator.toolbox.banned.user.ToolboxUserBanClient;
import org.triplea.http.client.moderator.toolbox.banned.user.UserBanParams;

/** Controller for endpoints to manage user bans, to be used by moderators. */
@Path("")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Builder
public class UserBanController {
  @Nonnull private final UserBanService bannedUsersService;

  @GET
  @Path(ToolboxUserBanClient.GET_USER_BANS_PATH)
  public Response getUserBans(@Context final HttpServletRequest request) {
    return Response.ok().entity(bannedUsersService.getBannedUsers()).build();
  }

  @POST
  @Path(ToolboxUserBanClient.REMOVE_USER_BAN_PATH)
  public Response removeUserBan(@Context final HttpServletRequest request, final String banId) {
    Preconditions.checkArgument(banId != null);

    // TODO: Project#12 grab moderator id from auth parameter
    final int moderatorId = 0;
    final boolean removed = bannedUsersService.removeUserBan(moderatorId, banId);
    return Response.status(removed ? 200 : 400).build();
  }

  /** Endpoint to add a user ban. Returns 200 if the ban is added, 400 if not. */
  @POST
  @Path(ToolboxUserBanClient.BAN_USER_PATH)
  public Response banUser(
      @Context final HttpServletRequest request, final UserBanParams banUserParams) {
    Preconditions.checkArgument(banUserParams != null);
    Preconditions.checkArgument(banUserParams.getHashedMac() != null);
    Preconditions.checkArgument(banUserParams.getIp() != null);
    Preconditions.checkArgument(banUserParams.getUsername() != null);
    Preconditions.checkArgument(banUserParams.getHoursToBan() > 0);

    // TODO: Project#12 grab moderator id from auth parameter
    final int moderatorId = 0;
    final boolean banned = bannedUsersService.banUser(moderatorId, banUserParams);
    return Response.status(banned ? 200 : 400).build();
  }
}
